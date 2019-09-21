import org.junit.jupiter.api.Test
import policymig.util.cloud.createComputeService
import policymig.util.cloud.fetchEc2Instances
import policymig.util.cloud.fetchInstancesFromGcp
import kotlin.streams.asSequence
import kotlin.system.measureNanoTime

class CloudTest {
    @Test
    fun verifyGcpInstances() {
        fetchInstancesFromGcp("pelagic-cycle-239905", createComputeService("/home/aayush/IdeaProjects/PolicyMig/gcp_auth.json"))
    }

    @Test
    fun verifyAwsInstances() {
        fetchEc2Instances()
    }
}