package ir.raha.hotspot
import org.junit.Test
import org.junit.Assert.*
class PolicyTest {
 @Test fun all() { assertTrue(Policy(Mode.ALL,emptySet()).blocks("a")) }
 @Test fun allow() { val p=Policy(Mode.ALLOW,setOf("a")); assertFalse(p.blocks("a")); assertTrue(p.blocks("b")) }
 @Test fun block() { val p=Policy(Mode.BLOCK,setOf("a")); assertTrue(p.blocks("a")); assertFalse(p.blocks("b")) }
 @Test(expected=IllegalArgumentException::class) fun emptyBlock() { Policy(Mode.BLOCK,emptySet()) }
}