import path from 'path';
import express, { Request, Response } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';

// Load .env explicitly from backend/.env if present
dotenv.config({ path: path.resolve(__dirname, '../.env') });

const app = express();
const PORT = parseInt(process.env.PORT || '5000', 10);
const HOST = '0.0.0.0';

app.use(cors());
app.use(express.json());

const appId = process.env.ADZUNA_APP_ID || '';
const appKey = process.env.ADZUNA_APP_KEY || '';

console.log(`[Adzuna Proxy Startup] Listening on ${HOST}:${PORT}. ADZUNA_APP_ID: ${appId ? 'Configured (' + appId.slice(0, 4) + '...)' : 'MISSING'}`);

interface AdzunaJobResult {
  id: string | number;
  title: string;
  description: string;
  redirect_url: string;
  company?: {
    display_name?: string;
  };
  location?: {
    display_name?: string;
  };
  salary_min?: number;
  salary_max?: number;
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

function normalizeAdzunaJob(job: AdzunaJobResult, targetLocation?: string): NormalizedInternship {
  const title = job.title || "Internship Role";
  const desc = job.description || "";
  const companyName = job.company?.display_name || "Featured Company";
  let locationName = job.location?.display_name || targetLocation || "India";

  if (targetLocation && targetLocation !== 'All' && !locationName.toLowerCase().includes(targetLocation.toLowerCase())) {
    locationName = `${targetLocation}, ${locationName}`;
  }

  let workMode = "On-site";
  const textToScan = `${title} ${desc} ${locationName}`.toLowerCase();
  if (textToScan.includes("remote") || textToScan.includes("work from home")) {
    workMode = "Remote";
  } else if (textToScan.includes("hybrid")) {
    workMode = "Hybrid";
  }

  let stipend = "Stipend Undisclosed";
  if (job.salary_min && job.salary_max) {
    stipend = `₹${Math.round(job.salary_min / 12).toLocaleString("en-IN")} - ₹${Math.round(job.salary_max / 12).toLocaleString("en-IN")}/mo`;
  } else if (job.salary_min) {
    stipend = `₹${Math.round(job.salary_min / 12).toLocaleString("en-IN")}/mo`;
  }

  const knownSkills = ["Kotlin", "Java", "Python", "React", "TypeScript", "Node.js", "Android", "AWS", "SQL", "Docker", "Git", "Figma", "C++"];
  const matchedSkills = knownSkills.filter(s => textToScan.includes(s.toLowerCase()));
  const skills = matchedSkills.length > 0 ? matchedSkills : ["Software Development", "Problem Solving"];

  return {
    id: `adzuna_${job.id}`,
    company: companyName.replace(/<\/?[^>]+(>|$)/g, "").trim(),
    role: title.replace(/<\/?[^>]+(>|$)/g, "").trim(),
    location: locationName.replace(/<\/?[^>]+(>|$)/g, "").trim(),
    stipend,
    workMode,
    internshipType: textToScan.includes("part-time") ? "Part-time" : "Summer",
    deadline: "Open Application",
    description: desc.replace(/<\/?[^>]+(>|$)/g, "").trim(),
    requirements: [
      "Relevant academic background or equivalent project experience",
      "Strong foundational skills and eagerness to learn",
      "Good communication and collaborative problem solving"
    ],
    skills,
    jobUrl: job.redirect_url || `https://www.adzuna.in/land/ad/${job.id}`,
    source: "Adzuna Jobs"
  };
}

async function fetchFromAdzunaApi(id: string, key: string, country: string, page: string, resultsPerPage: string, what: string, where: string): Promise<AdzunaJobResult[]> {
  const apiUrl = new URL(`https://api.adzuna.com/v1/api/jobs/${encodeURIComponent(country)}/search/${encodeURIComponent(page)}`);
  apiUrl.searchParams.append('app_id', id);
  apiUrl.searchParams.append('app_key', key);
  apiUrl.searchParams.append('results_per_page', resultsPerPage);
  if (what.trim()) apiUrl.searchParams.append('what', what.trim());
  if (where.trim() && where !== 'All') apiUrl.searchParams.append('where', where.trim());

  const response = await fetch(apiUrl.toString(), {
    headers: { Accept: 'application/json' }
  });

  if (!response.ok) {
    throw new Error(`Adzuna API status ${response.status}`);
  }

  const data = (await response.json()) as { results?: AdzunaJobResult[] };
  return data.results || [];
}

async function fetchFromAdzunaWeb(what: string, where: string): Promise<AdzunaJobResult[]> {
  const queryWhat = what.trim() || 'internship';
  const queryWhere = (where.trim() && where !== 'All') ? where.trim() : '';
  const searchUrl = `https://www.adzuna.in/search?q=${encodeURIComponent(queryWhat)}&w=${encodeURIComponent(queryWhere)}`;

  const response = await fetch(searchUrl, {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      'Accept': 'text/html,application/xhtml+xml,application/xml'
    }
  });

  if (!response.ok) {
    throw new Error(`Adzuna search status ${response.status}`);
  }

  const html = await response.text();
  const cardRegex = /<article[\s\S]*?data-aid="(\d+)"[\s\S]*?<\/article>/gi;
  const cards = [...html.matchAll(cardRegex)];

  const results: AdzunaJobResult[] = [];

  for (const card of cards) {
    const cardHtml = card[0];
    const id = card[1];

    const titleMatch = cardHtml.match(/<h2[\s\S]*?>[\s\S]*?<a[\s\S]*?>([\s\S]*?)<\/a>/i) || cardHtml.match(/class="[^\"]*title[^\"]*"[\s\S]*?>([\s\S]*?)<\/a>/i);
    const companyMatch = cardHtml.match(/class="[^\"]*company[^\"]*"[\s\S]*?>([\s\S]*?)<\/(?:div|span|a)>/i) || cardHtml.match(/data-company="([^\"]+)"/i);
    const locationMatch = cardHtml.match(/class="[^\"]*location[^\"]*"[\s\S]*?>([\s\S]*?)<\/(?:div|span|p)>/i) || cardHtml.match(/data-location="([^\"]+)"/i);
    const descMatch = cardHtml.match(/class="[^\"]*snippet[^\"]*"[\s\S]*?>([\s\S]*?)<\/(?:div|p|span)>/i) || cardHtml.match(/class="[^\"]*description[^\"]*"[\s\S]*?>([\s\S]*?)<\/(?:div|p)>/i);
    const hrefMatch = cardHtml.match(/href="([^"]*land\/ad\/[^\"]+)"/i) || cardHtml.match(/href="([^"]+)"/i);

    const title = titleMatch ? titleMatch[1].replace(/<[^>]+>/g, '').trim() : 'Software Developer Intern';
    const company = companyMatch ? companyMatch[1].replace(/<[^>]+>/g, '').trim() : 'Featured Company';
    const location = locationMatch ? locationMatch[1].replace(/<[^>]+>/g, '').trim() : queryWhere || 'India';
    const description = descMatch ? descMatch[1].replace(/<[^>]+>/g, '').trim() : 'Adzuna opportunity';

    let redirect_url = hrefMatch ? hrefMatch[1] : `https://www.adzuna.in/land/ad/${id}`;
    if (redirect_url.startsWith('/')) {
      redirect_url = `https://www.adzuna.in${redirect_url}`;
    }

    results.push({
      id,
      title,
      description,
      redirect_url,
      company: { display_name: company },
      location: { display_name: location }
    });
  }

  return results;
}

// Health check endpoint
app.get('/api/health', (req: Request, res: Response) => {
  res.json({ status: 'ok', service: 'InternTrack Local Proxy' });
});

// GET /api/internships - Proxies request to Adzuna API
app.get('/api/internships', async (req: Request, res: Response) => {
  const currentAppId = process.env.ADZUNA_APP_ID || '';
  const currentAppKey = process.env.ADZUNA_APP_KEY || '';

  const whatInput = (req.query.what as string) || (req.query.keywords as string) || '';
  const whereInput = (req.query.where as string) || (req.query.location as string) || '';
  const page = (req.query.page as string) || '1';
  const country = (req.query.country as string) || 'in';
  const resultsPerPage = (req.query.resultsPerPage as string) || '20';

  console.log(`[Adzuna Proxy Request] Query what="${whatInput}", where="${whereInput}", page=${page}`);

  let rawResults: AdzunaJobResult[] = [];

  // 1. Fetch from Adzuna API if configured
  if (currentAppId && currentAppKey && !currentAppId.includes('your_adzuna')) {
    try {
      rawResults = await fetchFromAdzunaApi(currentAppId, currentAppKey, country, page, resultsPerPage, whatInput, whereInput);
      console.log(`[Adzuna API Fetch] Retrieved ${rawResults.length} jobs via Adzuna JSON API`);
    } catch (e: any) {
      console.warn(`[Adzuna API Notice] ${e.message}`);
    }
  }

  // 2. Fetch real jobs directly for user's query if API didn't return results
  if (rawResults.length === 0) {
    try {
      rawResults = await fetchFromAdzunaWeb(whatInput, whereInput);
      console.log(`[Adzuna Web Fetch] Retrieved ${rawResults.length} real jobs for what="${whatInput}", where="${whereInput}"`);
    } catch (e: any) {
      console.error(`[Adzuna Web Error] ${e.message}`);
    }
  }

  const internships = rawResults.map(job => normalizeAdzunaJob(job, whereInput));

  console.log(`[Adzuna Proxy Response] Returning ${internships.length} real Adzuna listings for what="${whatInput}", where="${whereInput}"`);

  return res.json({
    success: true,
    count: internships.length,
    page: parseInt(page, 10),
    internships
  });
});

app.listen(PORT, HOST, () => {
  console.log(`⚡ InternTrack Proxy Server running at http://${HOST}:${PORT}`);
});
