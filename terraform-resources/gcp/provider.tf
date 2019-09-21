provider "google" {
	version = "~> 2.15.0"
	project = "pelagic-cycle-239905"
	credentials = file("/home/aayush/IdeaProjects/PolicyMig/gcp_auth.json")
}
