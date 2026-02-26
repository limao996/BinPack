import org.limao996.BinPack

fun main() {
    println("=== BinPack 示例程序 ===\n")

    // ─── 1. 基本整数和字符串 ─────────────────────────────────
    println("--- 1. 基本整数和字符串 ---")
    val data1 = BinPack.pack("<i32 z", 42, "hello")
    println("打包: i32=42, z=\"hello\" -> ${data1.toHexString()}")
    val result1 = BinPack.unpack("<i32 z", data1)
    println("解包: i32=${result1.values[0]}, z=\"${result1.values[1]}\"")
    println()

    // ─── 2. 字节序控制 ───────────────────────────────────────
    println("--- 2. 字节序控制 ---")
    val dataLE = BinPack.pack("<i16", 0x1234)
    val dataBE = BinPack.pack(">i16", 0x1234)
    println("小端序 i16=0x1234 -> ${dataLE.toHexString()}")
    println("大端序 i16=0x1234 -> ${dataBE.toHexString()}")
    println()

    // ─── 3. 浮点数 ──────────────────────────────────────────
    println("--- 3. 浮点数 ---")
    val data3 = BinPack.pack("<f16 f32 f64", 1.5, 3.14, 2.718281828459045)
    val result3 = BinPack.unpack("<f16 f32 f64", data3)
    println("f16=${result3.values[0]}, f32=${result3.values[1]}, f64=${result3.values[2]}")
    println()

    // ─── 4. 变长整数 ────────────────────────────────────────
    println("--- 4. 变长整数 ---")
    val data4 = BinPack.pack("v V v V", -1L, 127L, -128L, 300L)
    println("打包: v=-1, V=127, v=-128, V=300 -> ${data4.toHexString()} (${data4.size} 字节)")
    val result4 = BinPack.unpack("v V v V", data4)
    println("解包: v=${result4.values[0]}, V=${result4.values[1]}, v=${result4.values[2]}, V=${result4.values[3]}")
    println()

    // ─── 5. 变长前缀浮点数 (g) ──────────────────────────────
    println("--- 5. 变长前缀浮点数 (g) ---")
    val valZero = BinPack.pack("g", 0.0)
    val valF16 = BinPack.pack("g", 1.5)
    val valF32 = BinPack.pack("g", 3.14)
    val valF64 = BinPack.pack("g", 2.718281828459045)
    println("g=0.0   -> ${valZero.toHexString()} (${valZero.size} 字节)")
    println("g=1.5   -> ${valF16.toHexString()} (${valF16.size} 字节)")
    println("g=3.14  -> ${valF32.toHexString()} (${valF32.size} 字节)")
    println("g=2.718 -> ${valF64.toHexString()} (${valF64.size} 字节)")
    println("解包: ${BinPack.unpack("g", valF64).values[0]}")
    println()

    // ─── 6. 字符串类型 ──────────────────────────────────────
    println("--- 6. 字符串类型 ---")

    // z: 零字节结尾
    val dataZ = BinPack.pack("z", "hello")
    println("z=\"hello\" -> ${dataZ.toHexString()}")

    // s4: 4字节长度前缀
    val dataS = BinPack.pack("<s4", "world")
    println("s4=\"world\" -> ${dataS.toHexString()}")

    // c8: 定长字符数组
    val dataC = BinPack.pack("c8", "hi")
    println("c8=\"hi\" -> ${dataC.toHexString()}")

    // S: 变长前缀字符串 (原 p 格式)
    val dataSv = BinPack.pack("S", "你好世界")
    println("S=\"你好世界\" -> ${dataSv.toHexString()} (${dataSv.size} 字节)")
    val resultSv = BinPack.unpack("S", dataSv)
    println("解包 S: \"${resultSv.values[0]}\"")
    println()

    // ─── 7. 布尔值 ──────────────────────────────────────────
    println("--- 7. 布尔值 ---")

    // 单个布尔值
    val dataB1 = BinPack.pack("b b", true, false)
    val resultB1 = BinPack.unpack("b b", dataB1)
    println("b=true, b=false -> ${dataB1.toHexString()}")
    println("解包: ${resultB1.values[0]}, ${resultB1.values[1]}")

    // 定长布尔位数组
    val flags = booleanArrayOf(true, false, true, true, false, false, false, true)
    val dataB8 = BinPack.pack("b8", flags)
    println("b8=${flags.toList()} -> ${dataB8.toHexString()} (${dataB8.size} 字节)")
    val resultB8 = BinPack.unpack("b8", dataB8)
    println("解包 b8: ${(resultB8.values[0] as BooleanArray).toList()}")

    // 变长前缀布尔位数组
    val flags2 = booleanArrayOf(true, true, false, true, false)
    val dataBV = BinPack.pack("B", flags2)
    println("B=${flags2.toList()} -> ${dataBV.toHexString()} (${dataBV.size} 字节)")
    val resultBV = BinPack.unpack("B", dataBV)
    println("解包 B: ${(resultBV.values[0] as BooleanArray).toList()}")

    // 定长前缀布尔位数组
    val dataBF = BinPack.pack("B2", flags2)
    println("B2=${flags2.toList()} -> ${dataBF.toHexString()} (${dataBF.size} 字节)")
    val resultBF = BinPack.unpack("B2", dataBF)
    println("解包 B2: ${(resultBF.values[0] as BooleanArray).toList()}")
    println()

    // ─── 8. 字节数组 ────────────────────────────────────────
    println("--- 8. 字节数组 ---")
    val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())

    // a4: 定长字节数组
    val dataA = BinPack.pack("a4", bytes)
    println("a4 -> ${dataA.toHexString()}")

    // A: 变长前缀字节数组
    val dataAV = BinPack.pack("A", bytes)
    println("A (varint前缀) -> ${dataAV.toHexString()} (${dataAV.size} 字节)")
    val resultAV = BinPack.unpack("A", dataAV)
    println("解包 A: ${(resultAV.values[0] as ByteArray).toHexString()}")

    // A4: 4字节长度前缀字节数组
    val dataAF = BinPack.pack("<A4", bytes)
    println("A4 (4字节前缀) -> ${dataAF.toHexString()} (${dataAF.size} 字节)")
    println()

    // ─── 9. 填充字节 ────────────────────────────────────────
    println("--- 9. 填充字节 ---")
    val dataX = BinPack.pack("<i8 x4 i8", 0xAA, 0xBB)
    println("i8 x4 i8 -> ${dataX.toHexString()}")
    val resultX = BinPack.unpack("<i8 x4 i8", dataX)
    println("解包: ${resultX.values[0]}, ${resultX.values[1]}")
    println()

    // ─── 10. 无符号整数 ─────────────────────────────────────
    println("--- 10. 有符号/无符号整数 ---")
    val dataI = BinPack.pack("<i8 I8", -1, 255)
    val resultI = BinPack.unpack("<i8 I8", dataI)
    println("i8=-1 -> 解包=${resultI.values[0]}")
    println("I8=255 -> 解包=${resultI.values[1]}")
    println()

    // ─── 11. 大小预测 ───────────────────────────────────────
    println("--- 11. 大小预测 ---")
    val pred1 = BinPack.predictSize("<i32 f64 b")
    println("\"<i32 f64 b\" -> fixedBytes=${pred1.fixedBytes}, exact=${pred1.exact}")

    val pred2 = BinPack.predictSize("<i32 v S")
    println("\"<i32 v S\" -> fixedBytes=${pred2.fixedBytes}, variableFields=${pred2.variableFields}, exact=${pred2.exact}")

    val size = BinPack.computeSize("<i32 v S", 42, 100L, "hello")
    println("computeSize(\"<i32 v S\", 42, 100, \"hello\") = $size 字节")
    println()

    // ─── 12. 复合结构示例 ───────────────────────────────────
    println("--- 12. 复合结构示例 ---")
    val fmt = "<I8 S f32 B v"
    val packed = BinPack.pack(
        fmt, 1,                                          // I8: 版本号
        "player_001",                               // S: 玩家ID
        98.5,                                       // f32: 血量
        booleanArrayOf(true, false, true, true),    // B: 状态标志
        -42L                                        // v: 分数偏移
    )
    println("格式: \"$fmt\"")
    println("打包: ${packed.toHexString()} (${packed.size} 字节)")

    val unpacked = BinPack.unpack(fmt, packed)
    println("解包:")
    println("  版本号 (I8): ${unpacked.values[0]}")
    println("  玩家ID (S): \"${unpacked.values[1]}\"")
    println("  血量 (f32): ${unpacked.values[2]}")
    println("  状态标志 (B): ${(unpacked.values[3] as BooleanArray).toList()}")
    println("  分数偏移 (v): ${unpacked.values[4]}")
    println("  nextOffset: ${unpacked.nextOffset}")
    println()

    // ─── 13. 连续解包 ───────────────────────────────────────
    println("--- 13. 连续解包 ---")
    val multi = BinPack.pack("<i16", 100) + BinPack.pack("<i16", 200) + BinPack.pack("<i16", 300)
    var offset = 0
    for (idx in 1..3) {
        val r = BinPack.unpack("<i16", multi, offset)
        println("第${idx}次解包: value=${r.values[0]}, nextOffset=${r.nextOffset}")
        offset = r.nextOffset
    }
}

/**
 * 将 ByteArray 转换为十六进制字符串表示。
 */
fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
