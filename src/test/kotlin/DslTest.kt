import policymig.model.Policy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import policymig.util.FILE_EXTENSION
import policymig.util.dsl.policy
import policymig.util.io.readFromPcl
import policymig.util.io.writeToPcl
import java.io.File

class DslTest {
    companion object {
        @BeforeAll @JvmStatic
        fun clearPclFile() = File("sample_policy$FILE_EXTENSION").writeText("")
    }

    @Test
    fun verifyFileReadWrite() {
        val policy = policy {
            name = "test-policy"
            description = "Testing policy"
            target = "gcp"
            network = "default"
            direction = "INGRESS"
            sourceTags = mapOf("app" to "jarviss", "role" to "test")
            targetTags = mapOf("app" to "jarviss", "role" to "db", "env" to "mysql")
            rules {
                rule {
                    ports = listOf("8080", "5500-5600")
                    action = "allow"
                    protocol = "sctp"
                }
                rule {
                    ports = listOf("8080", "5500-5600")
                    action = "deny"
                    protocol = "udp"
                }
            }
        }
        val policies: MutableList<Policy> = mutableListOf(policy, policy.translatePolicy("aws", region="us-west-2"))
        policies.writeToPcl("sample_policy$FILE_EXTENSION")

        assertEquals(policies, readFromPcl("sample_policy$FILE_EXTENSION"))
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
//                sourceTags = mapOf("app" to "jarviss", "role" to "test")
                targetTags = mapOf("app" to "jarviss", "role" to "db", "env" to "mysql")
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