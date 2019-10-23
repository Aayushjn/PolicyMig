# PolicyMig

PolicyMig automates network security policy translation across cloud-based environments. Currently, it supports AWS and 
GCP cloud environments for security policy migration.

Policies are defined in JSON format which dictates the syntax for defining security policies across cloud targets.

___
### Policy Configuration
Each policy (.json file) has the following format:
```json
[
    {
        "name": "policy-name",
        "description": "Brief description",
        "target": "aws",
        "direction": "INGRESS",
        "region": "us-east-2",
        "sourceIps": [
            "10.10.35.48/16",
            "35.101.45.0/24"
        ],
        "sourceTags": [
            {"app": "App"},
            {"role": "db"},
            {"env": "MySQL"},
            {"ver": "8.5"}
        ],
        "rules": [
            {
                "action": "allow",
                "protocol": "tcp",
                "ports": [
                    "80",
                    "443",
                    "22"
                ]
            },
            {
                "action": "allow",
                "protocol": "udp",
                "ports": [
                    "5500-5600"
                ]
            }
        ]
    }
]
```
All policies must follow JSON array style.

The _name_ may contain only **alphanumerics and hyphens**.<br>
The _target_ may be either **aws** or **gcp**.<br>
The _direction_ must be either **INGRESS** or **EGRESS**.<br>
_network_ is specified only if target is **gcp**. It must be the name of the target network on GCP.<br>
_region_ is specified only if target is **aws**. It must be one of the regions specified by 
[AWS](src/main/kotlin/policymig/util/PolicyUtils.kt).
_sourceIps_ is a list of IPs. It must be specified only if direction is **INGRESS**.<br>
_sourceTags_ is a list of "key=value" pairs that are user-defined. If both _sourceIps_ and _sourceTags_ are provided,  
priority is given to _sourceTags_.<br>
_targetIps_ is a list of IPs. It must be specified only if direction is **EGRESS**.<br>
_targetTags_ is a list of "key=value" pairs that are user-defined. If both _targetIps_ and _targetTags_ are provided, 
priority is given to _targetTags_.<br>

Each rule's _action_ is either **allow** or **deny**.
The protocol must be one of those mentioned [here](src/main/kotlin/policymig/util/PolicyUtils.kt). **all** is invalid 
for GCP, while **sctp**, **esp** and **ah** are invalid for AWS.<br>
_ports_ may be a range or a list of singular port numbers.

___
### Build & Run
Clone this repo using the following command:<br>
`git clone https://github.com/Aayushjn/PolicyMig.git`

Import the project as a Gradle project and build using IDE or run:<br>
`./gradlew build`

Finally, to execute the output JAR file, run:<br>
```shell script
chmod +x cloud-mig.jar
java -jar cloud-mig.jar <command> <options> <arguments>
```
___
### Checklist
- [x] Policy translation
- [x] Policy DSL
    - [x] Write to file
    - [x] Read from file
- [x] Terraform configuration generation
    - [x] GCP
    - [x] AWS
- [x] Cloud discovery
    - [x] GCP
    - [x] AWS
- [x] Policy creation
    - [x] GCP
    - [x] AWS

