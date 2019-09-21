resource "google_compute_firewall" "firewall-b38sdugv" {
	name = "test-policy0"
	network = "default"
	direction = "EGRESS"
	description = "Testing policy"
	destination_ranges = ["0.0.0.0/0"]
	allow {
		protocol = "sctp"
		ports = ["8080", "5500-5600"]
	}
}

resource "google_compute_firewall" "firewall-97fh2x31" {
	name = "test-policy1"
	network = "default"
	direction = "EGRESS"
	description = "Testing policy"
	destination_ranges = ["0.0.0.0/0"]
	deny {
		protocol = "udp"
		ports = ["8080", "5500-5600"]
	}
}

