package com.hozinking.hacklab

/**
 * A tiny simulated terminal. It never runs a real shell and never touches
 * the real filesystem: every path resolves inside the virtual tree from
 * [Steps.buildFileSystem].
 *
 * Session state kept here is exactly what mission predicates read:
 * - [ran]: command names executed at least once.
 * - [history]: command names in execution order.
 * - [readFiles]: canonical virtual paths of files successfully read.
 * - [lsDirs]: canonical virtual paths of directories listed with ls.
 */
class TerminalEngine {

    private val root: VNode.Dir = Steps.buildFileSystem()

    var cwd: String = Steps.HOME_DIR
        private set

    val ran = LinkedHashSet<String>()
    val history = ArrayList<String>()
    val readFiles = LinkedHashSet<String>()
    val lsDirs = LinkedHashSet<String>()

    /** Short directory name for the prompt: "~" for home, else last segment. */
    fun promptDir(): String {
        if (cwd == Steps.HOME_DIR) return "~"
        if (cwd == "/") return "/"
        return cwd.substringAfterLast("/")
    }

    fun execute(rawInput: String): EngineResult {
        val input = rawInput.trim()
        if (input.isEmpty()) return EngineResult.Text("")
        val parts = input.split(Regex("\\s+"))
        val cmd = parts[0]
        val args = parts.drop(1)
        if (!KNOWN_COMMANDS.contains(cmd)) {
            return EngineResult.Text(
                "Perintah '$cmd' belum dikenal di simulator ini. " +
                    "Ketik help untuk melihat daftar perintah."
            )
        }
        ran.add(cmd)
        history.add(cmd)
        return when (cmd) {
            "help" -> EngineResult.Text(HELP_TEXT)
            "pwd" -> EngineResult.Text(cwd)
            "whoami" -> EngineResult.Text("pemula")
            "echo" -> EngineResult.Text(args.joinToString(" "))
            "clear" -> EngineResult.Clear
            "ls" -> runLs(args.firstOrNull())
            "cd" -> runCd(args.firstOrNull())
            "cat" -> runCat(args.firstOrNull())
            else -> EngineResult.Text("Perintah '$cmd' belum didukung.")
        }
    }

    private fun runLs(arg: String?): EngineResult {
        val resolved = resolve(arg ?: cwd) ?: return EngineResult.Text(
            "Folder atau file '${arg ?: ""}' tidak ditemukan."
        )
        return when (val node = resolved.node) {
            is VNode.File -> {
                EngineResult.Text(resolved.path.substringAfterLast("/"))
            }
            is VNode.Dir -> {
                lsDirs.add(resolved.path)
                if (node.children.isEmpty()) {
                    EngineResult.Text("(kosong)")
                } else {
                    val lines = ArrayList<String>()
                    for ((name, child) in node.children) {
                        if (child is VNode.Dir) lines.add("$name/") else lines.add(name)
                    }
                    EngineResult.Text(lines.joinToString("\n"))
                }
            }
        }
    }

    private fun runCd(arg: String?): EngineResult {
        if (arg == null) {
            cwd = Steps.HOME_DIR
            return EngineResult.Text("Sekarang kamu di $cwd.")
        }
        val resolved = resolve(arg) ?: return EngineResult.Text(
            "Folder '$arg' tidak ditemukan di sini."
        )
        return when (resolved.node) {
            is VNode.File -> EngineResult.Text(
                "Itu file, bukan folder. cd hanya untuk pindah ke folder."
            )
            is VNode.Dir -> {
                cwd = resolved.path
                EngineResult.Text("Sekarang kamu di $cwd.")
            }
        }
    }

    private fun runCat(arg: String?): EngineResult {
        if (arg == null) {
            return EngineResult.Text(
                "Tulis nama file setelah cat. Contoh: cat catatan.txt"
            )
        }
        val resolved = resolve(arg) ?: return EngineResult.Text(
            "File '$arg' tidak ditemukan."
        )
        return when (val node = resolved.node) {
            is VNode.Dir -> EngineResult.Text(
                "Itu folder, bukan file. Pakai cd untuk masuk ke folder; " +
                    "cat hanya untuk membaca file."
            )
            is VNode.File -> {
                readFiles.add(resolved.path)
                EngineResult.Text(node.content)
            }
        }
    }

    /**
     * Resolves a path (absolute or relative to [cwd]) inside the virtual
     * tree. Returns the node plus its canonical absolute path, or null
     * when any segment does not exist or passes through a file.
     */
    private fun resolve(path: String): Resolved? {
        val segments = ArrayList<String>()
        if (!path.startsWith("/")) {
            for (part in cwd.split("/")) {
                if (part.isNotEmpty()) segments.add(part)
            }
        }
        for (part in path.split("/")) {
            when (part) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments.add(part)
            }
        }
        var current: VNode = root
        for (segment in segments) {
            if (current !is VNode.Dir) return null
            val next = current.children[segment] ?: return null
            current = next
        }
        val canonical = if (segments.isEmpty()) "/" else "/" + segments.joinToString("/")
        return Resolved(current, canonical)
    }

    private data class Resolved(val node: VNode, val path: String)

    companion object {
        private val KNOWN_COMMANDS = setOf(
            "help", "pwd", "ls", "cd", "cat", "echo", "whoami", "clear"
        )

        private const val HELP_TEXT =
            "Perintah yang dikenal simulator ini:\n" +
                "  help          menampilkan daftar ini\n" +
                "  pwd           menunjukkan folder tempat kamu berada\n" +
                "  ls            menampilkan isi folder\n" +
                "  cd <folder>   pindah ke folder lain\n" +
                "  cat <file>    membaca isi file teks\n" +
                "  echo <teks>   menampilkan teks yang kamu tulis\n" +
                "  whoami        menunjukkan nama penggunamu\n" +
                "  clear         membersihkan layar"
    }
}

sealed class EngineResult {
    class Text(val text: String) : EngineResult()
    object Clear : EngineResult()
}
