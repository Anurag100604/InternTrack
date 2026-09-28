package com.example.interntrack.data

data class Opportunity(
    val id: String,
    val company: String,
    val role: String,
    val location: String,
    val stipend: String,
    val workMode: String, // Remote, Hybrid, On-site
    val internshipType: String, // Summer, Full-time, Part-time
    val deadline: String,
    val description: String,
    val requirements: List<String>,
    val skills: List<String>,
    val jobUrl: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "company" to company,
            "role" to role,
            "location" to location,
            "stipend" to stipend,
            "workMode" to workMode,
            "internshipType" to internshipType,
            "deadline" to deadline,
            "description" to description,
            "requirements" to requirements,
            "skills" to skills,
            "jobUrl" to jobUrl
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(docId: String, map: Map<String, Any?>): Opportunity {
            return Opportunity(
                id = (map["id"] as? String)?.ifBlank { docId } ?: docId,
                company = map["company"] as? String ?: "",
                role = map["role"] as? String ?: "",
                location = map["location"] as? String ?: "",
                stipend = map["stipend"] as? String ?: "",
                workMode = map["workMode"] as? String ?: "",
                internshipType = map["internshipType"] as? String ?: "",
                deadline = map["deadline"] as? String ?: "",
                description = map["description"] as? String ?: "",
                requirements = (map["requirements"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                skills = (map["skills"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                jobUrl = map["jobUrl"] as? String ?: ""
            )
        }
    }
}

object OpportunityDataSource {
    val curatedOpportunities: List<Opportunity> = listOf(
        Opportunity(
            id = "opp_1",
            company = "Google",
            role = "Software Engineering Intern",
            location = "Bangalore",
            stipend = "₹1,00,000/mo",
            workMode = "Hybrid",
            internshipType = "Summer",
            deadline = "30 Sep 2026",
            description = "Work on scalable distributed systems, cloud computing, and core services impacting billions of global users.",
            requirements = listOf(
                "Enrolled in B.Tech / M.Tech in CS/IT or related STEM field",
                "Strong foundation in Data Structures, Algorithms, and System Design",
                "Proficiency in Kotlin, Java, C++, or Python"
            ),
            skills = listOf("Data Structures", "Algorithms", "Kotlin/Java", "Git")
        ),
        Opportunity(
            id = "opp_2",
            company = "Microsoft",
            role = "Cloud & DevOps Intern",
            location = "Hyderabad",
            stipend = "₹80,000/mo",
            workMode = "Remote",
            internshipType = "Full-time",
            deadline = "15 Oct 2026",
            description = "Build and automate cloud infrastructure using Microsoft Azure, Docker, and continuous integration pipelines.",
            requirements = listOf(
                "Experience with Linux environments and basic scripting",
                "Familiarity with cloud platforms (Azure/AWS) and Docker",
                "Good problem solving and analytical thinking"
            ),
            skills = listOf("Azure", "Docker", "Linux", "CI/CD")
        ),
        Opportunity(
            id = "opp_3",
            company = "Amazon",
            role = "Android Developer Intern",
            location = "Hyderabad",
            stipend = "₹75,000/mo",
            workMode = "Hybrid",
            internshipType = "Summer",
            deadline = "10 Oct 2026",
            description = "Develop customer-facing mobile features for the Amazon Shopping and Prime Video Android apps using Jetpack Compose.",
            requirements = listOf(
                "Knowledge of Android SDK, Kotlin, Jetpack Compose, and Material 3",
                "Understanding of MVVM architecture and Room database",
                "Experience with REST APIs and background task handling"
            ),
            skills = listOf("Kotlin", "Jetpack Compose", "Room DB", "REST APIs")
        ),
        Opportunity(
            id = "opp_4",
            company = "Adobe",
            role = "Frontend Engineer Intern",
            location = "Noida",
            stipend = "₹65,000/mo",
            workMode = "Remote",
            internshipType = "Summer",
            deadline = "20 Oct 2026",
            description = "Build responsive and accessible digital experiences for Creative Cloud web applications using modern frameworks.",
            requirements = listOf(
                "Proficiency in modern TypeScript/JavaScript, React, and CSS/Tailwind",
                "Understanding of Web Performance and accessibility standards",
                "Passion for intuitive UI and creative tool workflows"
            ),
            skills = listOf("React", "TypeScript", "UI/UX", "Tailwind CSS")
        ),
        Opportunity(
            id = "opp_5",
            company = "Flipkart",
            role = "Mobile App Developer Intern",
            location = "Bangalore",
            stipend = "₹55,000/mo",
            workMode = "On-site",
            internshipType = "Full-time",
            deadline = "25 Oct 2026",
            description = "Optimize Flipkart's mobile checkout funnel, payments integration, and app responsiveness for millions of daily shoppers.",
            requirements = listOf(
                "Experience building Android or multiplatform mobile applications",
                "Familiarity with SQLite, local persistence, and offline caching",
                "Demonstrated academic or personal projects on GitHub"
            ),
            skills = listOf("Android", "Kotlin", "State Management", "Git")
        ),
        Opportunity(
            id = "opp_6",
            company = "Uber",
            role = "Backend Engineering Intern",
            location = "Bangalore",
            stipend = "₹90,000/mo",
            workMode = "Hybrid",
            internshipType = "Full-time",
            deadline = "05 Nov 2026",
            description = "Develop microservices powering real-time dispatch, route optimization, and pricing engines.",
            requirements = listOf(
                "Strong grasp of concurrency, networking protocols, and databases",
                "Experience with Go, Java, or Node.js",
                "Familiarity with Kafka or distributed queue systems"
            ),
            skills = listOf("Go/Java", "Microservices", "SQL", "Distributed Systems")
        ),
        Opportunity(
            id = "opp_7",
            company = "Spotify",
            role = "Data Science & ML Intern",
            location = "Remote",
            stipend = "₹70,000/mo",
            workMode = "Remote",
            internshipType = "Part-time",
            deadline = "12 Nov 2026",
            description = "Research recommendation algorithms and personalized playlists using audio feature vectors and user telemetry.",
            requirements = listOf(
                "Strong background in Python, PyTorch/TensorFlow, and statistics",
                "Experience analyzing large datasets with Pandas and SQL",
                "Curiosity for recommendation systems and ML research"
            ),
            skills = listOf("Python", "Machine Learning", "SQL", "Pandas")
        ),
        Opportunity(
            id = "opp_8",
            company = "Cisco",
            role = "Network Security Intern",
            location = "Bangalore",
            stipend = "₹50,000/mo",
            workMode = "On-site",
            internshipType = "Summer",
            deadline = "18 Nov 2026",
            description = "Analyze threat telemetry, test enterprise firewall integrations, and automate penetration testing suites.",
            requirements = listOf(
                "Knowledge of TCP/IP, OSI model, routing, and switching",
                "Basic understanding of cryptography and cyber defense",
                "Scripting skills in Python or Bash"
            ),
            skills = listOf("Networking", "Cybersecurity", "Python", "Linux")
        )
    )
}
