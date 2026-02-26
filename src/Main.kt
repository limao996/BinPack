import org.limao996.BinPack

fun main() {
    println("═══════════════════════════════════════════")
    println("           BinPack 示例程序")
    println("═══════════════════════════════════════════")

    // ─── 1. 布尔值 ────────────────────────────────────────────

    println("\n【1】单个布尔值 (b)")
    run {
        val data = BinPack.pack("bb", true, false)
        println("  pack(\"bb\", true, false) -> ${data.toHexString()}")
        val (values, _) = BinPack.unpack("bb", data)
        println("  unpack -> $values")
        // [true, false]
    }

    // ─── 2. 定长布尔位数组 ───────────────────────────────────

    println("\n【2】定长布尔位数组 (b[n])")
    run {
        val bools = booleanArrayOf(true, false, true, true, false, false, false, true)
        val data = BinPack.pack("b8", bools)
        println("  pack(\"b8\", [T,F,T,T,F,F,F,T]) -> ${data.toHexString()}")
        // 位排列 LSB first: bit0=T, bit1=F, bit2=T, bit3=T, bit4=F, bit5=F, bit6=F, bit7=T
        // = 0b10001101 = 0x8D
        val (values, _) = BinPack.unpack("b8", data)
        val arr = values[0] as BooleanArray
        println("  unpack -> ${arr.toList()}")
    }

    println("\n【2b】不足 8 位的布尔位数组 (b3)")
    run {
        val bools = booleanArrayOf(true, true, false)
        val data = BinPack.pack("b3", bools)
        println("  pack(\"b3\", [T,T,F]) -> ${data.toHexString()} (1 字节, 高5位补零)")
        val (values, _) = BinPack.unpack("b3", data)
        val arr = values[0] as BooleanArray
        println("  unpack -> ${arr.toList()}")
    }

    // ─── 3. 变长前缀布尔位数组 ──────────────────────────────

    println("\n【3】变长前缀布尔位数组 (B)")
    run {
        val bools = booleanArrayOf(true, false, true, false, true)
        val data = BinPack.pack("B", bools)
        println("  pack(\"B\", [T,F,T,F,T]) -> ${data.toHexString()}")
        println("  (varint前缀=5, 位数据=1字节)")
        val (values, _) = BinPack.unpack("B", data)
        val arr = values[0] as BooleanArray
        println("  unpack -> ${arr.toList()}")
    }

    // ─── 4. 定长前缀布尔位数组 ──────────────────────────────

    println("\n【4】定长前缀布尔位数组 (B[n])")
    run {
        val bools = booleanArrayOf(true, true, false, true, false, false, true, true, false, true)
        val data = BinPack.pack("B2", bools)
        println("  pack(\"B2\", 10个布尔值) -> ${data.toHexString()}")
        println("  (2字节前缀=10, 位数据=2字节)")
        val (values, _) = BinPack.unpack("B2", data)
        val arr = values[0] as BooleanArray
        println("  unpack -> ${arr.toList()}")
    }

    // ─── 5. 整数类型 ────────────────────────────────────────

    println("\n【5】整数类型 (i/I)")
    run {
        val data = BinPack.pack("<i8 i16 i32 I32", 42L, -1000L, 100000L, 0xDEADBEEFL)
        println("  pack(\"<i8 i16 i32 I32\", 42, -1000, 100000, 0xDEADBEEF)")
        println("  -> ${data.toHexString()} (${data.size} 字节)")
        val (values, _) = BinPack.unpack("<i8 i16 i32 I32", data)
        println("  unpack -> $values")
    }

    println("\n【5b】大端序整数")
    run {
        val data = BinPack.pack(">i32", 0x01020304L)
        println("  pack(\">i32\", 0x01020304) -> ${data.toHexString()}")
        val (values, _) = BinPack.unpack(">i32", data)
        println("  unpack -> ${values.map { "0x${(it as Long).toString(16)}" }}")
    }

    // ─── 6. 浮点类型 ────────────────────────────────────────

    println("\n【6】浮点类型 (f16/f32/f64)")
    run {
        val data = BinPack.pack("<f16 f32 f64", 1.5, 3.14, 2.718281828459045)
        println("  pack(\"<f16 f32 f64\", 1.5, 3.14, 2.718281828...) -> ${data.toHexString()}")
        println("  (2+4+8 = ${data.size} 字节)")
        val (values, _) = BinPack.unpack("<f16 f32 f64", data)
        println("  unpack -> $values")
    }

    // ─── 7. 变长浮点 ────────────────────────────────────────

    println("\n【7】变长前缀浮点数 (g/n)")
    run {
        val zero = BinPack.pack("g", 0.0)
        val half = BinPack.pack("g", 1.5)         // float16 可无损表示
        val pi = BinPack.pack("g", 3.14f.toDouble()) // float32
        val e = BinPack.pack("n", 2.718281828459045)  // float64
        println("  0.0  -> ${zero.toHexString()} (${zero.size} 字节)")
        println("  1.5  -> ${half.toHexString()} (${half.size} 字节)")
        println("  3.14 -> ${pi.toHexString()} (${pi.size} 字节)")
        println("  e    -> ${e.toHexString()} (${e.size} 字节)")

        println("  unpack(0.0)  = ${BinPack.unpack("g", zero).values}")
        println("  unpack(1.5)  = ${BinPack.unpack("g", half).values}")
        println("  unpack(3.14) = ${BinPack.unpack("g", pi).values}")
        println("  unpack(e)    = ${BinPack.unpack("n", e).values}")
    }

    // ─── 8. 变长整数 ────────────────────────────────────────

    println("\n【8】变长整数 (v/V)")
    run {
        val values = listOf(0L, 1L, -1L, 127L, 128L, -128L, 10000L, -10000L)
        for (v in values) {
            val signed = BinPack.pack("v", v)
            val (decoded, _) = BinPack.unpack("v", signed)
            println("  v: $v -> ${signed.toHexString()} (${signed.size}字节) -> ${decoded[0]}")
        }
        println()
        val unsignedValues = listOf(0L, 1L, 127L, 128L, 255L, 10000L, 1000000L)
        for (v in unsignedValues) {
            val unsigned = BinPack.pack("V", v)
            val (decoded, _) = BinPack.unpack("V", unsigned)
            println("  V: $v -> ${unsigned.toHexString()} (${unsigned.size}字节) -> ${decoded[0]}")
        }
    }

    // ─── 9. 字符串类型 ──────────────────────────────────────

    println("\n【9】字符串类型")
    run {
        // z: 零结尾字符串
        val zData = BinPack.pack("z", "hello")
        println("  z: \"hello\" -> ${zData.toHexString()} (${zData.size} 字节)")
        println("  unpack -> ${BinPack.unpack("z", zData).values}")

        // s4: 4字节前缀字符串
        val sData = BinPack.pack("<s4", "你好世界")
        println("  s4: \"你好世界\" -> ${sData.toHexString()} (${sData.size} 字节)")
        println("  unpack -> ${BinPack.unpack("<s4", sData).values}")

        // c16: 定长字符数组
        val cData = BinPack.pack("c16", "fixed")
        println("  c16: \"fixed\" -> ${cData.toHexString()} (${cData.size} 字节)")
        println("  unpack -> ${BinPack.unpack("c16", cData).values}")

        // p: 变长前缀字符串
        val pData = BinPack.pack("p", "varint prefixed string")
        println("  p: \"varint prefixed string\" -> (${pData.size} 字节)")
        println("  unpack -> ${BinPack.unpack("p", pData).values}")
    }

    // ─── 10. 字节数组类型 ───────────────────────────────────

    println("\n【10】字节数组类型")
    run {
        val raw = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())

        // a8: 定长字节数组
        val aData = BinPack.pack("a8", raw)
        println("  a8: [DE,AD,BE,EF] -> ${aData.toHexString()} (${aData.size} 字节, 补零至8)")
        val aResult = BinPack.unpack("a8", aData).values[0] as ByteArray
        println("  unpack -> ${aResult.toHexString()}")

        // A4: 4字节前缀字节数组
        val a4Data = BinPack.pack("<A4", raw)
        println("  A4: [DE,AD,BE,EF] -> ${a4Data.toHexString()} (${a4Data.size} 字节)")
        val a4Result = BinPack.unpack("<A4", a4Data).values[0] as ByteArray
        println("  unpack -> ${a4Result.toHexString()}")

        // A: 变长前缀字节数组
        val avData = BinPack.pack("A", raw)
        println("  A:  [DE,AD,BE,EF] -> ${avData.toHexString()} (${avData.size} 字节)")
        val avResult = BinPack.unpack("A", avData).values[0] as ByteArray
        println("  unpack -> ${avResult.toHexString()}")
    }

    // ─── 11. 填充 ───────────────────────────────────────────

    println("\n【11】填充 (x)")
    run {
        val data = BinPack.pack("b x4 b", true, false)
        println("  pack(\"b x4 b\", true, false) -> ${data.toHexString()} (${data.size} 字节)")
        val (values, _) = BinPack.unpack("b x4 b", data)
        println("  unpack -> $values")
    }

    // ─── 12. 复合格式 ──────────────────────────────────────

    println("\n【12】复合格式示例")
    run {
        // 模拟一个简单的数据包: 版本(i8) + 标志位(b8) + 名称(p) + 分数(f32) + 附加数据(A)
        val flags = booleanArrayOf(true, false, true, false, false, false, false, true)
        val extra = byteArrayOf(0x01, 0x02, 0x03)

        val packet = BinPack.pack("<i8 b8 p f32 A", 1L, flags, "Player1", 99.5, extra)
        println("  格式: \"<i8 b8 p f32 A\"")
        println("  参数: version=1, flags=[T,F,T,F,F,F,F,T], name=\"Player1\", score=99.5, extra=[01,02,03]")
        println("  pack -> ${packet.toHexString()} (${packet.size} 字节)")

        val (values, nextOffset) = BinPack.unpack("<i8 b8 p f32 A", packet)
        println("  unpack:")
        println("    version = ${values[0]}")
        println("    flags   = ${(values[1] as BooleanArray).toList()}")
        println("    name    = ${values[2]}")
        println("    score   = ${values[3]}")
        println("    extra   = ${(values[4] as ByteArray).toHexString()}")
        println("    nextOffset = $nextOffset")
    }

    // ─── 13. 大小预测与计算 ─────────────────────────────────

    println("\n【13】大小预测与计算")
    run {
        // 固定大小格式
        val pred1 = BinPack.predictSize("<i32 f64 b b8")
        println("  predictSize(\"<i32 f64 b b8\"):")
        println("    fixedBytes=${pred1.fixedBytes}, variableFields=${pred1.variableFields}, exact=${pred1.exact}")
        // 4 + 8 + 1 + 1 = 14

        // 含变长字段
        val pred2 = BinPack.predictSize("<i32 v p")
        println("  predictSize(\"<i32 v p\"):")
        println("    fixedBytes=${pred2.fixedBytes}, variableFields=${pred2.variableFields}, exact=${pred2.exact}")

        // 精确计算
        val size = BinPack.computeSize("<i32 v p", 0L, 42L, "hello")
        println("  computeSize(\"<i32 v p\", 0, 42, \"hello\") = $size")

        // 验证
        val actual = BinPack.pack("<i32 v p", 0L, 42L, "hello")
        println("  实际 pack 大小 = ${actual.size}")
        println("  匹配: ${size == actual.size}")
    }

    // ─── 14. 连续解包 ──────────────────────────────────────

    println("\n【14】连续解包 (使用 nextOffset)")
    run {
        val part1 = BinPack.pack("<i32 p", 100L, "first")
        val part2 = BinPack.pack("<f32 b", 1.0, true)
        val combined = part1 + part2

        println("  combined = ${combined.toHexString()} (${combined.size} 字节)")

        val r1 = BinPack.unpack("<i32 p", combined, 0)
        println("  第1次解包 (offset=0):  values=${r1.values}, nextOffset=${r1.nextOffset}")

        val r2 = BinPack.unpack("<f32 b", combined, r1.nextOffset)
        println("  第2次解包 (offset=${r1.nextOffset}): values=${r2.values}, nextOffset=${r2.nextOffset}")
    }

    println("\n═══════════════════════════════════════════")
    println("           所有示例执行完毕")
    println("═══════════════════════════════════════════")
}

// ─── 辅助函数 ──────────────────────────────────────────────

/**
 * 将 ByteArray 转换为十六进制字符串显示。
 */
private fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
