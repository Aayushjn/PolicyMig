# PolicyMig

PolicyMig automates network security policy translation across cloud-based environments. Currently, it supports AWS and 
GCP cloud environments for security policy migration.

It also defines a configuration language, termed Policy Configuration Language (.pcl), which dictates the syntax for 
defining security policies across cloud targets.

___
### Policy Configuration Language
Each policy (.pcl file) has the following format:
```
policy {
    name = "policy-name"
    description = "Brief description"
    target = "aws"
    direction = "INGRESS"
//    network = "app-subnet"
    region = "us-east-2"
    sourceIps = ["10.10.35.48/16", "35.101.45.0/24"]
//    sourceTags = ["app=App", "role=db", "env=MySQL", "ver=8.5"]
//    targetIps = ["0.0.0.0/0"]
//    targetTags = ["role=conf"]
    rules {
        rule {
            action = "allow"
            protocol = "tcp"
            ports = ["80", "443", "22"]
        }
        rule {
            action = "deny"
            protocol = "udp"
            ports = ["5500-5600"]
        }
    }
}
```

The _name_ may contain only **alphanumerics and hyphens**.<br>
The _target_ may be either **aws** or **gcp**.<br>
The _direction_ must be either **INGRESS** or **EGRESS**.<br>
_network_ is specified only if target is **gcp**. It must be the name of the target network on GCP.<br>
_region_ is specified only if target is **aws**. It must be one of the regions specified by 
[AWS](src/main/kotlin/policymig/util/PolicyUtils.kt).
_sourceIps_ is a list of IPs. It must be specified only if direction is **INGRESS**.<br>
_sourceTags_ is a list of "key=value" pairs that are user-defined. Either _sourceIps_ or _sourceTags_ must be provided 
(not both).<br>
_targetIps_ is a list of IPs. It must be specified only if direction is **EGRESS**.<br>
_targetTags_ is a list of "key=value" pairs that are user-defined. Either _targetIps_ or _targetTags_ must be provided 
(not both).<br>

Each rule's _action_ is either **allow** or **deny**.
The protocol must be one of those mentioned [here](src/main/kotlin/policymig/util/PolicyUtils.kt). **all** is invalid 
for GCP, while **sctp**, **esp** and **ah** are invalid for AWS.<br>
_ports_ may be a range or a list of singular port numbers.

___
### Checklist
- [x] Policy translation
- [ ] Policy DSL
    - [x] Write to file
    - [ ] Read from file
- [ ] Terraform configuration generation
    - [x] GCP
    - [ ] AWS
- [x] Cloud discovery
    - [x] GCP
    - [x] AWS
- [ ] Policy creation
    - [x] GCP
    - [ ] AWS
