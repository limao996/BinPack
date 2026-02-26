# BinPack

基于 Lua 5.3 风格 `string.pack` / `string.unpack` 的 Kotlin 二进制打包工具，支持整数、浮点数、变长编码、字符串、字节数组、布尔位数组等多种数据类型的序列化与反序列化。

## 特性

- 🔢 **整数**：支持任意 8 的倍数位宽的有符号/无符号整数
- 🔣 **变长整数**：ZigZag + LEB128 编码，自动压缩小数值
- 📐 **浮点数**：支持 float16 / float32 / float64 及自适应变长浮点编码
- 📝 **字符串**：零结尾、定长、固定前缀、变长前缀等多种编码方式
- 📦 **字节数组**：定长和带长度前缀的字节数组
- ✅ **布尔值**：单字节布尔值和紧凑位数组编码
- 🔀 **字节序**：支持小端序、大端序和本机原生字节序
- 📏 **大小预测**：支持静态预测和精确计算打包后的字节大小

## 快速开始

```kotlin
import org.limao996.BinPack

// 打包
val data = BinPack.pack("<i32 S f32", 42, "hello", 3.14)

// 解包
val result = BinPack.unpack("<i32 S f32", data)
val id = result.values[0] as Long       // 42
val name = result.values[1] as String   // "hello"
val score = result.values[2] as Double  // 3.14
```

## 格式字符串参考

格式字符串由一系列**格式符**组成，格式符之间可以插入空格（会被忽略）。

### 字节序控制

不消耗参数，影响后续所有字段的字节序。

| 格式符 | 说明 |
|--------|------|
| `<` | 小端序（Little-Endian），**默认值** |
| `>` | 大端序（Big-Endian） |
| `=` | 本机原生字节序 |

```kotlin
val le = BinPack.pack("<i16", 0x1234)  // -> [34, 12]
val be = BinPack.pack(">i16", 0x1234)  // -> [12, 34]
```

### 填充

不消耗参数。

| 格式符 | 说明 |
|--------|------|
| `x[n]` | 写入/跳过 `n` 个零字节（默认 1） |

```kotlin
BinPack.pack("<i8 x4 i8", 0xAA, 0xBB)  // -> [AA, 00, 00, 00, 00, BB]
```

### 整数类型

参数/返回类型：`Long`

| 格式符 | 说明 | 字节数 |
|--------|------|--------|
| `i[n]` | 有符号整数，`n` 位（默认 32） | n/8 |
| `I[n]` | 无符号整数，`n` 位（默认 32） | n/8 |

`n` 必须为 8 的正整数倍。

```kotlin
BinPack.pack("<i8 i16 i32 i64", 1, 2, 3, 4)
BinPack.pack("<I8 I16", 255, 65535)
```

### 浮点类型

参数/返回类型：`Double`

| 格式符 | 说明 | 字节数 |
|--------|------|--------|
| `f[n]` | 定长浮点数，`n` 位（默认 32） | n/8 |

支持 `f16`（半精度）、`f32`（单精度）、`f64`（双精度）。

```kotlin
BinPack.pack("<f16 f32 f64", 1.5, 3.14, 2.718281828459045)
```

### 变长整数类型

参数/返回类型：`Long`

| 格式符 | 说明 | 编码方式 |
|--------|------|----------|
| `v` | 有符号变长整数 | ZigZag + LEB128 |
| `V` | 无符号变长整数 | LEB128 |

小数值占用更少字节，适合数值范围不确定的场景。

```kotlin
BinPack.pack("v V", -42L, 300L)
```

**编码大小参考：**

| 数值范围 | 字节数 |
|----------|--------|
| 0 ~ 127 | 1 |
| 128 ~ 16383 | 2 |
| 16384 ~ 2097151 | 3 |
| ... | ... |

### 变长浮点类型

参数/返回类型：`Double`

| 格式符 | 说明 |
|--------|------|
| `g` | 变长前缀浮点数，自动选择最紧凑编码 |

使用 1 字节前缀标签指示后续数据格式：

| 前缀 | 格式 | 总字节数 | 条件 |
|------|------|----------|------|
| `0x00` | 零值 | 1 | 值为 0.0 |
| `0x01` | float16 | 3 | float16 无精度损失 |
| `0x02` | float32 | 5 | float32 无精度损失 |
| `0x03` | float64 | 9 | 其他情况 |

```kotlin
BinPack.pack("g", 0.0)                  // 1 字节
BinPack.pack("g", 1.5)                  // 3 字节 (float16)
BinPack.pack("g", 3.14)                 // 5 字节 (float32)
BinPack.pack("g", 2.718281828459045)    // 9 字节 (float64)
```

### 字符串类型

参数/返回类型：`String`

| 格式符 | 说明 |
|--------|------|
| `z` | 零字节结尾字符串（C 风格 null-terminated） |
| `s[n]` | 带 `n` 字节无符号整数长度前缀的字符串（默认 4） |
| `c[n]` | 定长字符数组，固定 `n` 字节，不足补零（必须指定 `n`） |
| `S` | 变长前缀字符串（使用无符号 varint 编码长度前缀） |

```kotlin
BinPack.pack("z", "hello")         // 68 65 6C 6C 6F 00
BinPack.pack("<s4", "hello")       // 05 00 00 00 68 65 6C 6C 6F
BinPack.pack("<s2", "hello")       // 05 00 68 65 6C 6C 6F
BinPack.pack("c8", "hi")           // 68 69 00 00 00 00 00 00
BinPack.pack("S", "hello")         // 05 68 65 6C 6C 6F
BinPack.pack("S", "你好世界")       // 0C E4 BD A0 E5 A5 BD E4 B8 96 E7 95 8C
```

### 字节数组类型

参数/返回类型：`ByteArray`

| 格式符 | 说明 |
|--------|------|
| `a[n]` | 定长字节数组，固定 `n` 字节，不足补零（必须指定 `n`） |
| `A[n]` | 带 `n` 字节无符号整数长度前缀的字节数组（默认 4） |
| `A` | 带无符号 varint 长度前缀的字节数组 |

```kotlin
val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())

BinPack.pack("a4", bytes)     // DE AD BE EF
BinPack.pack("A", bytes)      // 04 DE AD BE EF (varint 前缀)
BinPack.pack("<A4", bytes)    // 04 00 00 00 DE AD BE EF (4字节前缀)
```

### 布尔类型

| 格式符 | 说明 | 参数/返回类型 |
|--------|------|---------------|
| `b` | 单个布尔值，占 1 字节 | `Boolean` |
| `b[n]` | 定长布尔位数组，`n` 个布尔值编码为 n/8 字节 | `BooleanArray` |
| `B` | 变长前缀布尔位数组（varint 编码数量） | `BooleanArray` |
| `B[n]` | 定长前缀布尔位数组（`n` 字节整数编码数量） | `BooleanArray` |

#### 位数组编码规则

布尔值按索引顺序依次填入字节的低位到高位（LSB first）。不足 8 的倍数时，末尾字节的高位补零。

```
布尔值: [true, false, true, true, false, false, false, true]
索引:     0      1      2      3      4      5      6      7
位:       1      0      1      1      0      0      0      1
字节:   0b10001101 = 0x8D
```

```kotlin
// 单个布尔值
BinPack.pack("b b", true, false)  // -> [01, 00]

// 定长位数组：8 个布尔值 -> 1 字节
val flags = booleanArrayOf(true, false, true, true, false, false, false, true)
BinPack.pack("b8", flags)  // -> [8D]

// 变长前缀位数组
val bits = booleanArrayOf(true, true, false, true, false)
BinPack.pack("B", bits)    // -> [05, 0B] (varint 数量 + 位数据)

// 定长前缀位数组
BinPack.pack("B2", bits)   // -> [00, 05, 0B] (2字节数量 + 位数据)
```

## 解包

`unpack` 返回 `UnpackResult`，包含值列表和下一个未读取字节的偏移量：

```kotlin
val data = BinPack.pack("<i32 S b", 42, "test", true)

val result = BinPack.unpack("<i32 S b", data)
println(result.values)      // [42, "test", true]
println(result.nextOffset)  // 下一个未读取字节的偏移量
```

### 连续解包

利用 `nextOffset` 实现连续解包：

```kotlin
val stream = BinPack.pack("<i16", 100) +
             BinPack.pack("<i16", 200) +
             BinPack.pack("<i16", 300)

var offset = 0
repeat(3) {
    val r = BinPack.unpack("<i16", stream, offset)
    println(r.values[0])  // 100, 200, 300
    offset = r.nextOffset
}
```

### 带偏移量解包

```kotlin
val data = byteArrayOf(0x00, 0x00, 0x2A, 0x00, 0x00, 0x00)
val result = BinPack.unpack("<i32", data, offset = 2)
println(result.values[0])  // 42
```

## 大小预测

### 静态预测（不需要实际值）

```kotlin
// 所有字段固定大小
val pred = BinPack.predictSize("<i32 f64 b")
println(pred.fixedBytes)  // 13
println(pred.exact)       // 13

// 包含变长字段
val pred2 = BinPack.predictSize("<i32 v S")
println(pred2.fixedBytes)       // 4
println(pred2.variableFields)   // 2
println(pred2.exact)            // null (无法确定)
```

### 精确计算（需要实际值）

```kotlin
val size = BinPack.computeSize("<i32 v S", 42, 100L, "hello")
println(size)  // 精确字节数
```

## 综合示例

### 网络协议消息

```kotlin
// 定义消息格式：版本(1字节) + 消息类型(1字节) + 消息体(变长)
fun packMessage(type: Int, body: String): ByteArray {
    return BinPack.pack("<I8 I8 S", 1, type, body)
}

fun unpackMessage(data: ByteArray): Triple<Long, Long, String> {
    val r = BinPack.unpack("<I8 I8 S", data)
    return Triple(
        r.values[0] as Long,
        r.values[1] as Long,
        r.values[2] as String
    )
}

val msg = packMessage(1, "Hello, World!")
val (version, type, body) = unpackMessage(msg)
```

### 游戏存档

```kotlin
data class PlayerSave(
    val name: String,
    val level: Int,
    val hp: Float,
    val position: Triple<Double, Double, Double>,
    val inventory: BooleanArray  // 物品槽位是否有物品
)

fun packSave(save: PlayerSave): ByteArray {
    return BinPack.pack(
        "<S V f32 f64 f64 f64 B",
        save.name,
        save.level.toLong(),
        save.hp,
        save.position.first,
        save.position.second,
        save.position.third,
        save.inventory
    )
}

fun unpackSave(data: ByteArray): PlayerSave {
    val r = BinPack.unpack("<S V f32 f64 f64 f64 B", data)
    return PlayerSave(
        name = r.values[0] as String,
        level = (r.values[1] as Long).toInt(),
        hp = (r.values[2] as Double).toFloat(),
        position = Triple(
            r.values[3] as Double,
            r.values[4] as Double,
            r.values[5] as Double
        ),
        inventory = r.values[6] as BooleanArray
    )
}
```

### 文件头解析

```kotlin
// 解析 BMP 文件头
fun parseBmpHeader(data: ByteArray) {
    val r = BinPack.unpack("<c2 I32 I16 I16 I32", data)
    println("签名: ${r.values[0]}")          // "BM"
    println("文件大小: ${r.values[1]}")       // 文件总字节数
    println("保留1: ${r.values[2]}")
    println("保留2: ${r.values[3]}")
    println("数据偏移: ${r.values[4]}")       // 像素数据起始偏移
}
```

## 格式符速查表

| 格式符 | 类型 | 参数类型 | 固定大小 | 说明 |
|--------|------|----------|----------|------|
| `<` | 控制 | - | 0 | 小端序 |
| `>` | 控制 | - | 0 | 大端序 |
| `=` | 控制 | - | 0 | 本机字节序 |
| `x[n]` | 填充 | - | n | 零字节填充 |
| `i[n]` | 整数 | `Long` | n/8 | 有符号整数 |
| `I[n]` | 整数 | `Long` | n/8 | 无符号整数 |
| `v` | 整数 | `Long` | 变长 | 有符号变长整数 |
| `V` | 整数 | `Long` | 变长 | 无符号变长整数 |
| `f[n]` | 浮点 | `Double` | n/8 | 定长浮点数 |
| `g` | 浮点 | `Double` | 变长 | 变长前缀浮点数 |
| `z` | 字符串 | `String` | 变长 | 零结尾字符串 |
| `s[n]` | 字符串 | `String` | 变长 | n 字节前缀字符串 |
| `c[n]` | 字符串 | `String` | n | 定长字符数组 |
| `S` | 字符串 | `String` | 变长 | varint 前缀字符串 |
| `a[n]` | 字节数组 | `ByteArray` | n | 定长字节数组 |
| `A[n]` | 字节数组 | `ByteArray` | 变长 | n 字节前缀字节数组 |
| `A` | 字节数组 | `ByteArray` | 变长 | varint 前缀字节数组 |
| `b` | 布尔 | `Boolean` | 1 | 单个布尔值 |
| `b[n]` | 布尔 | `BooleanArray` | n/8 | 定长布尔位数组 |
| `B` | 布尔 | `BooleanArray` | 变长 | varint 前缀布尔位数组 |
| `B[n]` | 布尔 | `BooleanArray` | 变长 | n 字节前缀布尔位数组 |

## 许可证

MIT License
```