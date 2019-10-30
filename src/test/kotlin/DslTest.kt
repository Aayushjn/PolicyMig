import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import policymig.model.Policy
import policymig.util.FILE_EXTENSION
import policymig.util.dsl.policy
import policymig.util.io.readFromFile
import policymig.util.io.writeToFile
import java.io.File


const val fileName = "test_policy"

class DslTest {

    companion object {
        @BeforeAll @JvmStatic
        fun clearTestFile() = File("policies/$fileName$FILE_EXTENSION").writeText("")
    }

    @Test
    fun verifyPolicyTranslation() {
        val policy = policy {
            name = "test-policy"
            description = "Testing policy"
            target = "gcp"
            network = "default"
            direction = "INGRESS"
            sourceIps = listOf("192.168.2.0/16", "10.53.25.192/24")
//            targetIps = listOf("0.0.0.0/0")
            rules {
                rule {
                    ports = listOf("8080", "5500-5600")
                    action = "allow"
                    protocol = "tcp"
                }
                rule {
                    ports = listOf("3000")
                    action = "allow"
                    protocol = "udp"
                }
            }
        }

        val translatedPolicy = policy {
            name = "test-policy"
            description = "Testing policy"
            target = "aws"
            region = "us-west-2"
            direction = "INGRESS"
            sourceIps = listOf("192.168.2.0/16", "10.53.25.192/24")
//            targetIps = listOf("0.0.0.0/0")
            rules {
                rule {
                    ports = listOf("8080", "5500-5600")
                    action = "allow"
                    protocol = "tcp"
                }
                rule {
                    ports = listOf("3000")
                    action = "allow"
                    protocol = "udp"
                }
            }
        }

        assertEquals(translatedPolicy, policy.translatePolicy("aws", region = "us-west-2"))
    }

    @Test
    fun verifyFileReadWrite() {
        val policy = policy {
            name = "test-policy"
            description = "Testing policy"
            target = "gcp"
            network = "default"
            direction = "INGRESS"
//            sourceIps = listOf("192.168.2.0/16", "10.53.25.192/24")
            sourceTags = listOf("app" to "PolicyMig", "role" to "test-env", "dev" to "Kt-1.3.50")
//            targetIps = listOf("0.0.0.0/0")
            rules {
                rule {
                    ports = listOf("8080", "5500-5600")
                    action = "allow"
                    protocol = "sctp"
                }
                rule {
                    ports = listOf("3000")
                    action = "deny"
                    protocol = "udp"
                }
            }
        }

        val policies: MutableList<Policy> = mutableListOf(policy, policy.translatePolicy("aws", region="us-west-2"))
        policies.writeToFile(fileName + FILE_EXTENSION)

        assertEquals(policies, readFromFile(fileName + FILE_EXTENSION))
    }

    @Test
    fun handleInvalidPolicies() {
        assertThrows<IllegalArgumentException> {
            policy {
                name = "test-policy"
                description = "Testing policy"
                target = "gcp"
                network = "default"
                direction = "INGRESS"
                sourceIps = listOf("192.22.6.355")
//                sourceTags = listOf("app" to "PolicyMig", "role" to "test")
                targetTags = listOf("app" to "PolicyMig", "role" to "db", "env" to "mysql")
                rules {
                    rule {
                        ports = listOf("8080", "5500-5600")
                        action = "invalid"
                        protocol = "all"
                    }
                }
            }
        }
    }
}