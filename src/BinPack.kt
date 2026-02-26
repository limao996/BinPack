package org.limao996

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 基于 Lua 5.3 风格的 `string.pack` / `string.unpack` 的 Kotlin 二进制打包工具，
 * 支持整数、浮点数、变长编码、字符串、字节数组、布尔位数组等多种数据类型的序列化与反序列化。
 *
 * ## 格式字符串说明
 *
 * 格式字符串由一系列**格式符**组成，每个格式符描述一个字段的类型和编码方式。
 * 格式符之间可以插入空格，空格会被忽略。
 *
 * ### 字节序控制（不消耗参数）
 *
 * | 格式符 | 说明 |
 * |--------|------|
 * | `<`    | 小端序（Little-Endian），**默认值** |
 * | `>`    | 大端序（Big-Endian） |
 * | `=`    | 本机原生字节序 |
 *
 * ### 填充（不消耗参数）
 *
 * | 格式符  | 说明 |
 * |---------|------|
 * | `x[n]`  | 写入/跳过 `n` 个零字节（默认 1） |
 *
 * ### 布尔类型
 *
 * | 格式符  | 说明 | 参数/返回类型 |
 * |---------|------|---------------|
 * | `b`     | 单个布尔值，占 1 字节（`0x00` = false, `0x01` = true） | `Boolean` |
 * | `b[n]`  | 定长布尔位数组，`n` 个布尔值紧密编码为 ⌈n/8⌉ 字节的位数组，无前缀 | `BooleanArray` |
 * | `B`     | 变长前缀布尔位数组，使用无符号 varint 编码布尔值数量，后跟位数组数据 | `BooleanArray` |
 * | `B[n]`  | 定长前缀布尔位数组，使用 `n` 字节无符号整数编码布尔值数量，后跟位数组数据 | `BooleanArray` |
 *
 * #### 位数组编码规则
 *
 * 布尔值按索引顺序依次填入字节的低位到高位（LSB first）。
 * 例如 8 个布尔值 `[true, false, true, true, false, false, false, true]`
 * 编码为 1 字节 `0b10001101` = `0x8D`。
 * 不足 8 的倍数时，末尾字节的高位补零。
 *
 * ### 整数类型
 *
 * | 格式符  | 说明 | 字节数 | 参数/返回类型 |
 * |---------|------|--------|---------------|
 * | `i[n]`  | 有符号整数，`n` 位（默认 32），`n` 必须为 8 的正整数倍 | n/8 | `Long` |
 * | `I[n]`  | 无符号整数，`n` 位（默认 32），`n` 必须为 8 的正整数倍 | n/8 | `Long` |
 *
 * ### 浮点类型
 *
 * | 格式符  | 说明 | 字节数 | 参数/返回类型 |
 * |---------|------|--------|---------------|
 * | `f[n]`  | 定长浮点数，`n` 位（默认 32），支持 16/32/64 | n/8 | `Double` |
 *
 * ### 变长浮点类型
 *
 * | 格式符 | 说明 | 参数/返回类型 |
 * |--------|------|---------------|
 * | `g`    | 变长前缀浮点数（前缀标签：0=零值, 1=f16, 2=f32, 3=f64） | `Double` |
 * | `n`    | 等同于 `g` | `Double` |
 *
 * ### 变长整数类型
 *
 * | 格式符 | 说明 | 参数/返回类型 |
 * |--------|------|---------------|
 * | `v`    | 有符号变长整数（ZigZag + LEB128 编码） | `Long` |
 * | `V`    | 无符号变长整数（LEB128 编码） | `Long` |
 *
 * ### 字符串类型
 *
 * | 格式符  | 说明 | 参数/返回类型 |
 * |---------|------|---------------|
 * | `z`     | 零字节结尾字符串（C 风格 null-terminated） | `String` |
 * | `s[n]`  | 带 `n` 字节无符号整数长度前缀的字符串（默认 4 字节前缀） | `String` |
 * | `c[n]`  | 定长字符数组，固定 `n` 字节，不足补零，读取时去除尾部零字节（必须指定 `n`） | `String` |
 * | `p`     | 变长前缀字符串（使用无符号 varint 编码长度前缀） | `String` |
 *
 * ### 字节数组类型
 *
 * | 格式符  | 说明 | 参数/返回类型 |
 * |---------|------|---------------|
 * | `a[n]`  | 定长字节数组，固定 `n` 字节，不足补零（必须指定 `n`） | `ByteArray` |
 * | `A[n]`  | 带 `n` 字节无符号整数长度前缀的字节数组（默认 4 字节前缀） | `ByteArray` |
 * | `A`     | 带无符号 varint 长度前缀的字节数组 | `ByteArray` |
 *
 * ### 变长前缀浮点数编码策略（`g`/`n` 格式符）
 *
 * 使用 1 字节前缀标签指示后续数据格式：
 * - `0x00`：值为零，无后续数据（共 1 字节）
 * - `0x01`：float16 编码（共 3 字节）
 * - `0x02`：float32 编码（共 5 字节）
 * - `0x03`：float64 编码（共 9 字节）
 *
 * 编码时自动选择最紧凑且无精度损失的格式。
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 打包整数和字符串
 * val data = BinPack.pack("<i32z", 42, "hello")
 *
 * // 解包
 * val (values, nextOffset) = BinPack.unpack("<i32z", data)
 *
 * // 打包布尔值
 * val flags = BinPack.pack("b b8", true, booleanArrayOf(true, false, true, false, true, false, true, false))
 *
 * // 变长前缀布尔位数组
 * val dynamic = BinPack.pack("B", booleanArrayOf(true, true, false))
 *
 * // 定长前缀布尔位数组
 * val fixed = BinPack.pack("B2", booleanArrayOf(true, true, false))
 *
 * // 预测大小
 * val prediction = BinPack.predictSize("<i32")
 * println(prediction.exact) // 4
 *
 * // 计算精确大小（含变长字段）
 * val size = BinPack.computeSize("<vp", 42L, "hello")
 * ```
 */
object BinPack {

    // ─── 公共 API ──────────────────────────────────────────────

    /**
     * 根据格式字符串将多个值打包为字节数组。
     *
     * @param fmt 格式字符串
     * @param args 要打包的值，数量和类型必须与格式字符串中的字段一一对应
     * @return 打包后的字节数组
     * @throws IllegalArgumentException 格式字符串包含未知格式符，或参数不匹配
     */
    fun pack(fmt: String, vararg args: Any): ByteArray {
        val ops = parse(fmt)
        val out = ByteArrayOutputStream()
        var ai = 0
        var order = ByteOrder.LITTLE_ENDIAN

        for (op in ops) {
            when (op.code) {
                '<' -> order = ByteOrder.LITTLE_ENDIAN
                '>' -> order = ByteOrder.BIG_ENDIAN
                '=' -> order = ByteOrder.nativeOrder()
                'x' -> out.write(ByteArray(op.size))

                'b' -> {
                    if (op.size == 0) {
                        val v = args[ai++] as Boolean
                        out.write(if (v) 1 else 0)
                    } else {
                        val arr = args[ai++] as BooleanArray
                        out.writeBitArray(arr, op.size)
                    }
                }

                'B' -> {
                    val arr = args[ai++] as BooleanArray
                    if (op.size == 0) {
                        out.writeVarintUnsigned(arr.size.toLong())
                    } else {
                        out.writeInt(arr.size.toLong(), op.size, order)
                    }
                    out.writeBitArray(arr, arr.size)
                }

                'i' -> out.writeInt(args[ai++] as Number, op.size, order)
                'I' -> out.writeInt(args[ai++] as Number, op.size, order)

                'f' -> {
                    val v = (args[ai++] as Number).toDouble()
                    when (op.size) {
                        2 -> out.writeFloat16(v, order)
                        4 -> {
                            val buf = ByteBuffer.allocate(4).order(order)
                            buf.putFloat(v.toFloat())
                            out.write(buf.array())
                        }

                        8 -> {
                            val buf = ByteBuffer.allocate(8).order(order)
                            buf.putDouble(v)
                            out.write(buf.array())
                        }
                    }
                }

                'z' -> {
                    val s = args[ai++] as String
                    out.write(s.toByteArray(Charsets.UTF_8))
                    out.write(0)
                }

                's' -> {
                    val s = args[ai++] as String
                    val bytes = s.toByteArray(Charsets.UTF_8)
                    out.writeInt(bytes.size.toLong(), op.size, order)
                    out.write(bytes)
                }

                'c' -> {
                    val s = args[ai++] as String
                    val bytes = s.toByteArray(Charsets.UTF_8)
                    val fixed = ByteArray(op.size)
                    bytes.copyInto(fixed, 0, 0, minOf(bytes.size, op.size))
                    out.write(fixed)
                }

                'a' -> {
                    val src = args[ai++] as ByteArray
                    val fixed = ByteArray(op.size)
                    src.copyInto(fixed, 0, 0, minOf(src.size, op.size))
                    out.write(fixed)
                }

                'A' -> {
                    val src = args[ai++] as ByteArray
                    if (op.size == 0) {
                        out.writeVarintUnsigned(src.size.toLong())
                    } else {
                        out.writeInt(src.size.toLong(), op.size, order)
                    }
                    out.write(src)
                }

                'v' -> out.writeVarintSigned((args[ai++] as Number).toLong())
                'V' -> out.writeVarintUnsigned((args[ai++] as Number).toLong())
                'g', 'n' -> out.writeVarFloat((args[ai++] as Number).toDouble())
                'p' -> {
                    val s = args[ai++] as String
                    val bytes = s.toByteArray(Charsets.UTF_8)
                    out.writeVarintUnsigned(bytes.size.toLong())
                    out.write(bytes)
                }
            }
        }
        return out.toByteArray()
    }

    /**
     * 根据格式字符串从字节数组中解包数据。
     *
     * @param fmt 格式字符串
     * @param data 要解包的字节数组
     * @param offset 起始偏移量，默认为 0
     * @return [UnpackResult]，包含解包后的值列表和下一个未读取字节的偏移量
     * @throws IllegalArgumentException 格式字符串包含未知格式符
     * @throws IllegalStateException 数据格式不正确
     */
    fun unpack(fmt: String, data: ByteArray, offset: Int = 0): UnpackResult {
        val ops = parse(fmt)
        val inp = ByteArrayInputStream(data, offset, data.size - offset)
        val results = mutableListOf<Any>()
        var order = ByteOrder.LITTLE_ENDIAN

        for (op in ops) {
            when (op.code) {
                '<' -> order = ByteOrder.LITTLE_ENDIAN
                '>' -> order = ByteOrder.BIG_ENDIAN
                '=' -> order = ByteOrder.nativeOrder()
                'x' -> inp.skip(op.size.toLong())

                'b' -> {
                    if (op.size == 0) {
                        results.add(inp.read() != 0)
                    } else {
                        results.add(inp.readBitArray(op.size))
                    }
                }

                'B' -> {
                    val count: Int = if (op.size == 0) {
                        inp.readVarintUnsigned().toInt()
                    } else {
                        inp.readInt(op.size, order, signed = false).toInt()
                    }
                    results.add(inp.readBitArray(count))
                }

                'i' -> results.add(inp.readInt(op.size, order, signed = true))
                'I' -> results.add(inp.readInt(op.size, order, signed = false))

                'f' -> {
                    when (op.size) {
                        2 -> results.add(inp.readFloat16(order))
                        4 -> {
                            val buf = ByteArray(4)
                            inp.read(buf)
                            results.add(ByteBuffer.wrap(buf).order(order).getFloat().toDouble())
                        }

                        8 -> {
                            val buf = ByteArray(8)
                            inp.read(buf)
                            results.add(ByteBuffer.wrap(buf).order(order).getDouble())
                        }
                    }
                }

                'z' -> {
                    val sb = ByteArrayOutputStream()
                    while (true) {
                        val c = inp.read()
                        if (c <= 0) break
                        sb.write(c)
                    }
                    results.add(sb.toByteArray().toString(Charsets.UTF_8))
                }

                's' -> {
                    val len = inp.readInt(op.size, order, signed = false).toInt()
                    val buf = ByteArray(len)
                    inp.read(buf)
                    results.add(buf.toString(Charsets.UTF_8))
                }

                'c' -> {
                    val buf = ByteArray(op.size)
                    inp.read(buf)
                    val end = buf.indexOfFirst { it == 0.toByte() }.let { if (it < 0) buf.size else it }
                    results.add(String(buf, 0, end, Charsets.UTF_8))
                }

                'a' -> {
                    val buf = ByteArray(op.size)
                    inp.read(buf)
                    results.add(buf)
                }

                'A' -> {
                    val len: Int = if (op.size == 0) {
                        inp.readVarintUnsigned().toInt()
                    } else {
                        inp.readInt(op.size, order, signed = false).toInt()
                    }
                    val buf = ByteArray(len)
                    inp.read(buf)
                    results.add(buf)
                }

                'v' -> results.add(inp.readVarintSigned())
                'V' -> results.add(inp.readVarintUnsigned())
                'g', 'n' -> results.add(inp.readVarFloat())
                'p' -> {
                    val len = inp.readVarintUnsigned().toInt()
                    val buf = ByteArray(len)
                    inp.read(buf)
                    results.add(buf.toString(Charsets.UTF_8))
                }
            }
        }
        val consumed = data.size - offset - inp.available()
        return UnpackResult(results, offset + consumed)
    }

    // ─── 大小预测 ─────────────────────────────────────────────

    /**
     * 仅根据格式字符串预测打包后的字节大小（不需要实际的值）。
     *
     * 对于变长格式符（`v`、`V`、`g`、`n`、`p`、`z`、`B`（变长前缀）、`A`（变长前缀）等），
     * 无法确定精确大小，此时 [SizePrediction.exact] 为 `null`。
     *
     * @param fmt 格式字符串
     * @return [SizePrediction]，包含固定字节数、变长字段数和精确大小（如果可确定）
     */
    fun predictSize(fmt: String): SizePrediction {
        val ops = parse(fmt)
        var fixed = 0
        var variableCount = 0
        for (op in ops) {
            val s = fixedSizeOf(op)
            if (s == null) variableCount++ else fixed += s
        }
        return SizePrediction(
            fixedBytes = fixed, variableFields = variableCount, exact = if (variableCount == 0) fixed else null
        )
    }

    /**
     * 根据格式字符串和实际参数值计算精确的打包字节大小。
     *
     * @param fmt 格式字符串
     * @param args 要打包的值，数量和类型必须与格式字符串中的字段一一对应
     * @return 打包后的精确字节数
     */
    fun computeSize(fmt: String, vararg args: Any): Int {
        val ops = parse(fmt)
        var total = 0
        var ai = 0
        for (op in ops) {
            when (op.code) {
                '<', '>', '=' -> {}
                'x' -> total += op.size

                'b' -> {
                    if (op.size == 0) {
                        total += 1
                    } else {
                        total += bitArrayByteSize(op.size)
                    }
                    ai++
                }

                'B' -> {
                    val arr = args[ai++] as BooleanArray
                    if (op.size == 0) {
                        total += varintUnsignedSize(arr.size.toLong())
                    } else {
                        total += op.size
                    }
                    total += bitArrayByteSize(arr.size)
                }

                'i', 'I' -> {
                    total += op.size; ai++
                }

                'f' -> {
                    total += op.size; ai++
                }

                'z' -> {
                    total += (args[ai++] as String).toByteArray(Charsets.UTF_8).size + 1
                }

                's' -> {
                    total += op.size + (args[ai++] as String).toByteArray(Charsets.UTF_8).size
                }

                'c' -> {
                    total += op.size; ai++
                }

                'a' -> {
                    total += op.size; ai++
                }

                'A' -> {
                    val bytes = args[ai++] as ByteArray
                    if (op.size == 0) {
                        total += varintUnsignedSize(bytes.size.toLong())
                    } else {
                        total += op.size
                    }
                    total += bytes.size
                }

                'v' -> total += varintSignedSize((args[ai++] as Number).toLong())
                'V' -> total += varintUnsignedSize((args[ai++] as Number).toLong())
                'g', 'n' -> total += varFloatSize((args[ai++] as Number).toDouble())

                'p' -> {
                    val bytes = (args[ai++] as String).toByteArray(Charsets.UTF_8)
                    total += varintUnsignedSize(bytes.size.toLong()) + bytes.size
                }
            }
        }
        return total
    }

    // ─── 数据类 ────────────────────────────────────────────────

    /**
     * 解包操作的返回结果。
     *
     * @property values 解包后的值列表，按格式字符串中字段的顺序排列
     * @property nextOffset 下一个未读取字节在原始数据中的偏移量，可用于连续解包
     */
    data class UnpackResult(val values: List<Any>, val nextOffset: Int)

    /**
     * 大小预测结果。
     *
     * @property fixedBytes 所有固定大小字段的总字节数
     * @property variableFields 变长字段的数量
     * @property exact 精确的总字节数；仅当所有字段都是固定大小时为非 `null`
     */
    data class SizePrediction(
        val fixedBytes: Int, val variableFields: Int, val exact: Int?
    )

    // ─── 格式解析器 ───────────────────────────────────────────

    /**
     * 格式字符串中的单个操作符。
     *
     * @property code 格式字符
     * @property size 关联的大小参数；含义因格式符而异，无参数时为 0
     */
    private data class Op(val code: Char, val size: Int = 0)

    /**
     * 将格式字符串解析为操作符列表。
     *
     * @param fmt 格式字符串
     * @return 解析后的 [Op] 列表
     * @throws IllegalArgumentException 遇到未知格式字符，或缺少必需的大小参数
     */
    private fun parse(fmt: String): List<Op> {
        val ops = mutableListOf<Op>()
        var i = 0
        while (i < fmt.length) {
            val c = fmt[i++]
            when (c) {
                '<', '>', '=' -> ops.add(Op(c))

                'x' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    ops.add(Op(c, n.first.ifEmpty { "1" }.toInt()))
                }

                'b' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    if (n.first.isEmpty()) {
                        ops.add(Op(c, 0))
                    } else {
                        ops.add(Op(c, n.first.toInt()))
                    }
                }

                'B' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    if (n.first.isEmpty()) {
                        ops.add(Op(c, 0))
                    } else {
                        ops.add(Op(c, n.first.toInt()))
                    }
                }

                'f' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    val bits = n.first.ifEmpty { "32" }.toInt()
                    require(bits == 16 || bits == 32 || bits == 64) {
                        "f 格式符仅支持 f16、f32、f64，不支持 f$bits"
                    }
                    ops.add(Op(c, bits / 8))
                }

                'z' -> ops.add(Op(c))
                'v', 'V', 'g', 'n', 'p' -> ops.add(Op(c))

                'i', 'I' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    val bits = n.first.ifEmpty { "32" }.toInt()
                    require(bits % 8 == 0 && bits > 0) {
                        "i/I 格式符的位数必须为 8 的正整数倍，不支持 ${bits} 位"
                    }
                    ops.add(Op(c, bits / 8))
                }

                's' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    ops.add(Op(c, n.first.ifEmpty { "4" }.toInt()))
                }

                'A' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    if (n.first.isEmpty()) {
                        ops.add(Op(c, 0))
                    } else {
                        ops.add(Op(c, n.first.toInt()))
                    }
                }

                'c' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    require(n.first.isNotEmpty()) { "c 格式符必须指定大小，例如 c16" }
                    ops.add(Op(c, n.first.toInt()))
                }

                'a' -> {
                    val n = readDigits(fmt, i)
                    i += n.second
                    require(n.first.isNotEmpty()) { "a 格式符必须指定大小，例如 a16" }
                    ops.add(Op(c, n.first.toInt()))
                }

                ' ' -> {}

                else -> throw IllegalArgumentException("未知的格式字符: $c")
            }
        }
        return ops
    }

    /**
     * 从字符串的指定位置开始读取连续的数字字符。
     *
     * @return `Pair(数字字符串, 消耗的字符数)`
     */
    private fun readDigits(s: String, start: Int): Pair<String, Int> {
        var i = start
        while (i < s.length && s[i].isDigit()) i++
        return s.substring(start, i) to (i - start)
    }

    /**
     * 返回给定操作符的固定字节大小，变长格式符返回 `null`。
     */
    private fun fixedSizeOf(op: Op): Int? = when (op.code) {
        '<', '>', '=' -> 0
        'x' -> op.size
        'b' -> if (op.size == 0) 1 else bitArrayByteSize(op.size)
        'B' -> null
        'f' -> op.size
        'i', 'I' -> op.size
        's' -> null
        'c' -> op.size
        'a' -> op.size
        'A' -> null
        'z', 'v', 'V', 'g', 'n', 'p' -> null
        else -> null
    }

    // ─── 布尔位数组读写 ──────────────────────────────────────

    /**
     * 计算 [count] 个布尔值编码为位数组所需的字节数：⌈count / 8⌉。
     */
    private fun bitArrayByteSize(count: Int): Int = (count + 7) / 8

    /**
     * 将布尔数组编码为位数组写入输出流。
     *
     * 布尔值按索引顺序填入字节的低位到高位（LSB first），
     * 不足 8 的倍数时末尾字节高位补零。
     *
     * @param arr 布尔数组
     * @param count 要编码的布尔值数量
     */
    private fun ByteArrayOutputStream.writeBitArray(arr: BooleanArray, count: Int) {
        val byteCount = bitArrayByteSize(count)
        for (byteIdx in 0 until byteCount) {
            var byte = 0
            for (bitIdx in 0 until 8) {
                val boolIdx = byteIdx * 8 + bitIdx
                if (boolIdx < count && boolIdx < arr.size && arr[boolIdx]) {
                    byte = byte or (1 shl bitIdx)
                }
            }
            write(byte)
        }
    }

    /**
     * 从输入流中读取位数组并解码为布尔数组。
     *
     * @param count 布尔值数量
     * @return 解码后的布尔数组
     */
    private fun ByteArrayInputStream.readBitArray(count: Int): BooleanArray {
        val byteCount = bitArrayByteSize(count)
        val result = BooleanArray(count)
        for (byteIdx in 0 until byteCount) {
            val byte = read()
            for (bitIdx in 0 until 8) {
                val boolIdx = byteIdx * 8 + bitIdx
                if (boolIdx < count) {
                    result[boolIdx] = (byte and (1 shl bitIdx)) != 0
                }
            }
        }
        return result
    }

    // ─── 整数读写 ─────────────────────────────────────────────

    /**
     * 将整数值以指定字节数和字节序写入输出流。
     *
     * @param num 要写入的数值
     * @param bytes 写入的字节数
     * @param order 字节序
     */
    private fun ByteArrayOutputStream.writeInt(num: Number, bytes: Int, order: ByteOrder) {
        val v = num.toLong()
        val buf = ByteArray(bytes)
        for (j in 0 until bytes) {
            val shift = if (order == ByteOrder.LITTLE_ENDIAN) j * 8 else (bytes - 1 - j) * 8
            buf[j] = (v ushr shift).toByte()
        }
        write(buf)
    }

    /**
     * 从输入流中读取指定字节数的整数值。
     *
     * @param bytes 要读取的字节数
     * @param order 字节序
     * @param signed 是否进行符号扩展
     * @return 读取到的整数值
     */
    private fun ByteArrayInputStream.readInt(bytes: Int, order: ByteOrder, signed: Boolean): Long {
        val buf = ByteArray(bytes)
        read(buf)
        var v = 0L
        for (j in 0 until bytes) {
            val shift = if (order == ByteOrder.LITTLE_ENDIAN) j * 8 else (bytes - 1 - j) * 8
            v = v or ((buf[j].toLong() and 0xFF) shl shift)
        }
        if (signed && bytes < 8) {
            val signBit = 1L shl (bytes * 8 - 1)
            if (v and signBit != 0L) {
                v = v or (-1L shl (bytes * 8))
            }
        }
        return v
    }

    // ─── Float16 读写 ─────────────────────────────────────────

    /**
     * 将 Double 值编码为 IEEE 754 半精度浮点数（float16）并写入输出流。
     *
     * @param value 要编码的浮点数值
     * @param order 字节序
     */
    private fun ByteArrayOutputStream.writeFloat16(value: Double, order: ByteOrder) {
        val bits = doubleToFloat16Bits(value)
        val buf = ByteArray(2)
        if (order == ByteOrder.LITTLE_ENDIAN) {
            buf[0] = (bits and 0xFF).toByte()
            buf[1] = ((bits shr 8) and 0xFF).toByte()
        } else {
            buf[0] = ((bits shr 8) and 0xFF).toByte()
            buf[1] = (bits and 0xFF).toByte()
        }
        write(buf)
    }

    /**
     * 从输入流中读取 IEEE 754 半精度浮点数（float16）并转换为 Double。
     *
     * @param order 字节序
     * @return 解码后的 Double 值
     */
    private fun ByteArrayInputStream.readFloat16(order: ByteOrder): Double {
        val buf = ByteArray(2)
        read(buf)
        val bits = if (order == ByteOrder.LITTLE_ENDIAN) {
            (buf[0].toInt() and 0xFF) or ((buf[1].toInt() and 0xFF) shl 8)
        } else {
            ((buf[0].toInt() and 0xFF) shl 8) or (buf[1].toInt() and 0xFF)
        }
        return float16BitsToDouble(bits)
    }

    /**
     * 将 Double 值转换为 float16 的 16 位整数表示。
     */
    private fun doubleToFloat16Bits(value: Double): Int {
        val fbits = java.lang.Float.floatToIntBits(value.toFloat())
        val sign = (fbits ushr 16) and 0x8000
        var exponent = ((fbits ushr 23) and 0xFF) - 127
        val mantissa = fbits and 0x007FFFFF

        if (exponent == 128) {
            return sign or 0x7C00 or if (mantissa != 0) (mantissa ushr 13).coerceAtLeast(1) else 0
        }
        if (exponent < -24) {
            return sign
        }
        if (exponent < -14) {
            val shift = -14 - exponent
            val subnormalMantissa = (mantissa or 0x00800000) ushr (13 + shift)
            return sign or subnormalMantissa
        }
        if (exponent > 15) {
            return sign or 0x7C00
        }
        return sign or ((exponent + 15) shl 10) or (mantissa ushr 13)
    }

    /**
     * 将 float16 的 16 位整数表示转换为 Double 值。
     */
    private fun float16BitsToDouble(bits: Int): Double {
        val sign = (bits and 0x8000) != 0
        val exponent = (bits ushr 10) and 0x1F
        val mantissa = bits and 0x03FF

        val result: Double = when {
            exponent == 0 -> {
                if (mantissa == 0) 0.0
                else mantissa.toDouble() / 1024.0 * Math.pow(2.0, -14.0)
            }

            exponent == 31 -> {
                if (mantissa == 0) Double.POSITIVE_INFINITY
                else Double.NaN
            }

            else -> {
                (1.0 + mantissa.toDouble() / 1024.0) * Math.pow(2.0, (exponent - 15).toDouble())
            }
        }
        return if (sign) -result else result
    }

    // ─── 变长整数（LEB128 风格）───────────────────────────────

    /**
     * 将无符号整数以 LEB128 编码写入输出流。
     */
    private fun ByteArrayOutputStream.writeVarintUnsigned(value: Long) {
        var v = value
        do {
            var byte = (v and 0x7F).toInt()
            v = v ushr 7
            if (v != 0L) byte = byte or 0x80
            write(byte)
        } while (v != 0L)
    }

    /**
     * 将有符号整数以 ZigZag + LEB128 编码写入输出流。
     */
    private fun ByteArrayOutputStream.writeVarintSigned(value: Long) {
        writeVarintUnsigned((value shl 1) xor (value shr 63))
    }

    /**
     * 从输入流中读取 LEB128 编码的无符号整数。
     */
    private fun ByteArrayInputStream.readVarintUnsigned(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val b = read()
            result = result or ((b.toLong() and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result
    }

    /**
     * 从输入流中读取 ZigZag + LEB128 编码的有符号整数。
     */
    private fun ByteArrayInputStream.readVarintSigned(): Long {
        val raw = readVarintUnsigned()
        return (raw ushr 1) xor -(raw and 1)
    }

    /**
     * 计算无符号整数的 LEB128 编码所需字节数。
     */
    private fun varintUnsignedSize(value: Long): Int {
        var v = value
        var n = 1
        while (v ushr 7 != 0L) {
            n++; v = v ushr 7
        }
        return n
    }

    /**
     * 计算有符号整数的 ZigZag + LEB128 编码所需字节数。
     */
    private fun varintSignedSize(value: Long): Int = varintUnsignedSize((value shl 1) xor (value shr 63))

    // ─── 变长前缀浮点数 ──────────────────────────────────────

    /**
     * 将浮点数以变长前缀编码写入输出流。
     *
     * 自动选择最紧凑且无精度损失的编码：
     * - `0x00`：零值（1 字节）
     * - `0x01`：float16（3 字节）
     * - `0x02`：float32（5 字节）
     * - `0x03`：float64（9 字节）
     */
    private fun ByteArrayOutputStream.writeVarFloat(value: Double) {
        if (value == 0.0) {
            write(0x00)
            return
        }
        val f16bits = doubleToFloat16Bits(value)
        if (float16BitsToDouble(f16bits) == value) {
            write(0x01)
            val buf = ByteArray(2)
            buf[0] = (f16bits and 0xFF).toByte()
            buf[1] = ((f16bits shr 8) and 0xFF).toByte()
            write(buf)
            return
        }
        val f = value.toFloat()
        if (f.toDouble() == value) {
            write(0x02)
            val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            buf.putFloat(f)
            write(buf.array())
            return
        }
        write(0x03)
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buf.putDouble(value)
        write(buf.array())
    }

    /**
     * 从输入流中读取变长前缀编码的浮点数。
     *
     * @throws IllegalStateException 遇到无效的前缀标签
     */
    private fun ByteArrayInputStream.readVarFloat(): Double {
        return when (read()) {
            0x00 -> 0.0
            0x01 -> {
                val buf = ByteArray(2); read(buf)
                val bits = (buf[0].toInt() and 0xFF) or ((buf[1].toInt() and 0xFF) shl 8)
                float16BitsToDouble(bits)
            }

            0x02 -> {
                val buf = ByteArray(4); read(buf)
                ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getFloat().toDouble()
            }

            0x03 -> {
                val buf = ByteArray(8); read(buf)
                ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getDouble()
            }

            else -> throw IllegalStateException("无效的 varfloat 前缀标签")
        }
    }

    /**
     * 计算浮点数的变长前缀编码所需字节数。
     */
    private fun varFloatSize(value: Double): Int {
        if (value == 0.0) return 1
        val f16bits = doubleToFloat16Bits(value)
        if (float16BitsToDouble(f16bits) == value) return 1 + 2
        if (value.toFloat().toDouble() == value) return 1 + 4
        return 1 + 8
    }
}

