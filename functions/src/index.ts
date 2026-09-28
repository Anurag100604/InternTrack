import { setGlobalOptions } from "firebase-functions/v2";
import { onCall, onRequest, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";

admin.initializeApp();

setGlobalOptions({ region: "us-central1" });

// Define secrets from Firebase Secret Manager
const adzunaAppId = defineSecret("ADZUNA_APP_ID");
const adzunaAppKey = defineSecret("ADZUNA_APP_KEY");

export const healthCheck = onRequest((req, res) => {
  res.status(200).send({
    status: "ok",
    service: "InternTrack Cloud Functions",
    timestamp: new Date().toISOString()
  });
});

interface AdzunaJobResult {
  id: string | number;
  title: string;
  description: string;
  redirect_url: string;
  created: string;
  salary_min?: number;
  salary_max?: number;
  company?: {
    display_name?: string;
  };
  location?: {
    display_name?: string;
    area?: string[];
  };
}

interface NormalizedInternship {
  id: string;
  company: string;
  role: string;
  location: string;
  stipend: string;
  workMode: string;
  internshipType: string;
  deadline: string;
  description: string;
  requirements: string[];
  skills: string[];
  jobUrl: string;
  source: string;
}

/**
 * Normalizes Adzuna raw job result into InternTrack Opportunity schema.
 */
function normalizeAdzunaJob(job: AdzunaJobResult): NormalizedInternship {
  const title = job.title || "Internship Role";
  const desc = job.description || "";
  const companyName = job.company?.display_name || "Featured Company";
  const locationName = job.location?.display_name || "India";

  // Determine Work Mode
  let workMode = "On-site";
  const textToScan = `${title} ${desc} ${locationName}`.toLowerCase();
  if (textToScan.includes("remote") || textToScan.includes("work from home")) {
    workMode = "Remote";
  } else if (textToScan.includes("hybrid")) {
    workMode = "Hybrid";
  }

  // Determine Stipend/Salary
  let stipend = "Stipend Undisclosed";
  if (job.salary_min && job.salary_max) {
    stipend = `₹${Math.round(job.salary_min / 12).toLocaleString("en-IN")} - ₹${Math.round(job.salary_max / 12).toLocaleString("en-IN")}/mo`;
  } else if (job.salary_min) {
    stipend = `₹${Math.round(job.salary_min / 12).toLocaleString("en-IN")}/mo`;
  }

  // Extract skills from text
  const knownSkills = ["Kotlin", "Java", "Python", "React", "TypeScript", "Node.js", "Android", "AWS", "SQL", "Docker", "Git", "Figma", "C++"];
  const matchedSkills = knownSkills.filter(s => textToScan.includes(s.toLowerCase()));
  const skills = matchedSkills.length > 0 ? matchedSkills : ["Software Development", "Problem Solving"];

  return {
    id: `adzuna_${job.id}`,
    company: companyName.replace(/<\/?[^>]+(>|$)/g, ""),
    role: title.replace(/<\/?[^>]+(>|$)/g, ""),
    location: locationName.replace(/<\/?[^>]+(>|$)/g, ""),
    stipend,
    workMode,
    internshipType: textToScan.includes("part-time") ? "Part-time" : "Summer",
    deadline: "Open Application",
    description: desc.replace(/<\/?[^>]+(>|$)/g, ""),
    requirements: [
      "Relevant academic background or equivalent project experience",
      "Strong foundational skills and eagerness to learn",
      "Good communication and collaborative problem solving"
    ],
    skills,
    jobUrl: job.redirect_url || "",
    source: "Adzuna Jobs"
  };
}

/**
 * Callable Firebase Cloud Function to search internships securely via Adzuna API.
 */
export const searchInternships = onCall(
  {
    secrets: [adzunaAppId, adzunaAppKey],
    region: "us-central1"
  },
  async (request) => {
    // Check if user is authenticated
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "The searchInternships function requires user authentication."
      );
    }

    const appId = adzunaAppId.value();
    const appKey = adzunaAppKey.value();

    if (!appId || !appKey) {
      throw new HttpsError(
        "failed-precondition",
        "Adzuna API credentials are not configured in Secret Manager."
      );
    }

    const keywords = (request.data?.keywords as string) || "internship";
    const location = (request.data?.location as string) || "India";
    const page = (request.data?.page as number) || 1;
    const country = (request.data?.country as string) || "in";
    const resultsPerPage = (request.data?.resultsPerPage as number) || 20;

    const queryWhat = keywords.toLowerCase().includes("intern")
      ? keywords
      : `${keywords} internship`;

    const apiUrl = new URL(
      `https://api.adzuna.com/v1/api/jobs/${encodeURIComponent(country)}/search/${page}`
    );
    apiUrl.searchParams.append("app_id", appId);
    apiUrl.searchParams.append("app_key", appKey);
    apiUrl.searchParams.append("results_per_page", resultsPerPage.toString());
    apiUrl.searchParams.append("what", queryWhat);
    if (location && location !== "All") {
      apiUrl.searchParams.append("where", location);
    }

    try {
      const response = await fetch(apiUrl.toString(), {
        method: "GET",
        headers: {
          "Accept": "application/json"
        }
      });

      if (!response.ok) {
        throw new Error(`Adzuna API returned HTTP status ${response.status}`);
      }

      const data = (await response.json()) as { results?: AdzunaJobResult[] };
      const rawResults = data.results || [];
      const normalizedListings = rawResults.map(normalizeAdzunaJob);

      return {
        success: true,
        count: normalizedListings.length,
        page,
        internships: normalizedListings
      };
    } catch (error: any) {
      console.error("Error fetching Adzuna internships:", error);
      throw new HttpsError(
        "internal",
        `Failed to fetch internships from Adzuna API: ${error.message || error}`
      );
    }
  }
);
