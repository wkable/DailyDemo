package booster

import com.didiglobal.booster.transform.TransformContext
import com.didiglobal.booster.transform.asm.ClassTransformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

/**
 * 自定义 Transform
 */
class BoosterAsmPlugin : ClassTransformer {

    override fun transform(context: TransformContext, klass: ClassNode): ClassNode {
        klass.methods.forEach {
            handleNewThread(klass, it)
        }
        return klass
    }

    private fun handleNewThread(klass: ClassNode, methodNode: MethodNode) {
        methodNode.instructions.filter { it.opcode == Opcodes.NEW }.filterIsInstance<TypeInsnNode>()
            .filter { it.desc == THREAD }.forEach { typeNode ->
                (typeNode.find { node ->
                    node is MethodInsnNode && node.opcode == Opcodes.INVOKESPECIAL && node.name == "<init>"
                } as? MethodInsnNode)?.let {
                    it.owner = SHADOW_THREAD
                    val index = it.desc.indexOf(')')
                    it.desc = "${
                        it.desc.substring(0, index)
                    };Ljava/lang/String;${it.desc.substring(index)}"
                    methodNode.instructions.insertBefore(
                        it, LdcInsnNode(makeThreadName(klass.name))
                    )
                }
            }
    }
}

fun AbstractInsnNode.find(predicate: (AbstractInsnNode) -> Boolean): AbstractInsnNode? {
    var node: AbstractInsnNode? = this
    while (node != null) {
        if (predicate(node)) return node
        node = node.next
    }
    return null
}

internal const val MARK = "\u200B"
private fun makeThreadName(name: String) = MARK + name
