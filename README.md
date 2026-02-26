# BinPack

基于 Lua 5.3 风格 `string.pack` / `string.unpack` 的 **Kotlin 二进制打包工具**，支持整数、浮点数、变长编码、字符串、字节数组、布尔位数组等多种数据类型的序列化与反序列化。

## 特性

- 🔢 **整数类型** — 有符号/无符号，支持任意 8 的倍数位宽（8/16/24/32/…/64）
- 🔣 **浮点类型** — 支持 float16（半精度）、float32、float64
- 📦 **变长编码** — LEB128 无符号变长整数、ZigZag + LEB128 有符号变长整数、变长前缀浮点数
- 📝 **字符串** — 零结尾（C 风格）、定长前缀、变长前缀（varint）、定长字符数组
- 🧮 **布尔位数组** — 紧密位编码，支持定长/变长前缀
- 📐 **大小预测** — 无需实际数据即可预测打包大小，或根据实际参数精确计算
- 🔄 **字节序控制** — 小端序（默认）、大端序、本机原生字节序

## 快速开始

```kotlin
import org.limao996.BinPack

// 打包整数和字符串
val data = BinPack.pack("<i32 z", 42L, "hello")

// 解包
val (values, nextOffset) = BinPack.unpack("<i32 z", data)
println(values) // [42, hello]
```

## API 参考

### `BinPack.pack(fmt, vararg args): ByteArray`

根据格式字符串将多个值打包为字节数组。

```kotlin
val data = BinPack.pack("<i32 f64 p", 100L, 3.14, "hello")
```

### `BinPack.unpack(fmt, data, offset = 0): UnpackResult`

根据格式字符串从字节数组中解包数据。返回 `UnpackResult(values: List<Any>, nextOffset: Int)`。

```kotlin
val (values, nextOffset) = BinPack.unpack("<i32 f64 p", data)
```

### `BinPack.predictSize(fmt): SizePrediction`

仅根据格式字符串预测打包后的字节大小（不需要实际的值）。对于变长格式符，`exact` 为 `null`。

```kotlin
val prediction = BinPack.predictSize("<i32 f64 b")
println(prediction.exact) // 13 (4 + 8 + 1)
```

### `BinPack.computeSize(fmt, vararg args): Int`

根据格式字符串和实际参数值计算精确的打包字节大小。

```kotlin
val size = BinPack.computeSize("<v p", 42L, "hello")
```

## 格式字符串

格式字符串由一系列**格式符**组成，格式符之间可以插入空格（空格会被忽略）。

### 字节序控制

不消耗参数，影响后续所有字段的字节序。

| 格式符 | 说明 |
|--------|------|
| `<`    | 小端序（Little-Endian），**默认值** |
| `>`    | 大端序（Big-Endian） |
| `=`    | 本机原生字节序 |

### 填充

| 格式符  | 说明 | 消耗参数 |
|---------|------|----------|
| `x[n]`  | 写入/跳过 `n` 个零字节（默认 1） | 否 |

### 布尔类型

| 格式符  | 说明 | 参数类型 |
|---------|------|----------|
| `b`     | 单个布尔值，占 1 字节 | `Boolean` |
| `b[n]`  | 定长布尔位数组，`n` 个布尔值编码为 ⌈n/8⌉ 字节 | `BooleanArray` |
| `B`     | 变长前缀布尔位数组（varint 编码长度） | `BooleanArray` |
| `B[n]`  | 定长前缀布尔位数组（`n` 字节无符号整数编码长度） | `BooleanArray` |

> **位数组编码规则**：布尔值按索引顺序依次填入字节的低位到高位（LSB first）。例如 `[true, false, true, true, false, false, false, true]` 编码为 `0b10001101` = `0x8D`。不足 8 的倍数时末尾字节高位补零。

### 整数类型

| 格式符  | 说明 | 字节数 | 参数类型 |
|---------|------|--------|----------|
| `i[n]`  | 有符号整数，`n` 位（默认 32） | n/8 | `Long` |
| `I[n]`  | 无符号整数，`n` 位（默认 32） | n/8 | `Long` |

> `n` 必须为 8 的正整数倍。

### 浮点类型

| 格式符  | 说明 | 字节数 | 参数类型 |
|---------|------|--------|----------|
| `f[n]`  | 定长浮点数，`n` 位（默认 32） | n/8 | `Double` |

> 支持 `f16`（半精度）、`f32`（单精度）、`f64`（双精度）。

### 变长浮点类型

| 格式符 | 说明 | 参数类型 |
|--------|------|----------|
| `g`    | 变长前缀浮点数 | `Double` |
| `n`    | 等同于 `g` | `Double` |

编码策略（自动选择最紧凑且无精度损失的格式）：

| 前缀标签 | 格式 | 总字节数 |
|----------|------|----------|
| `0x00`   | 零值，无后续数据 | 1 |
| `0x01`   | float16 | 3 |
| `0x02`   | float32 | 5 |
| `0x03`   | float64 | 9 |

### 变长整数类型

| 格式符 | 说明 | 参数类型 |
|--------|------|----------|
| `v`    | 有符号变长整数（ZigZag + LEB128） | `Long` |
| `V`    | 无符号变长整数（LEB128） | `Long` |

### 字符串类型

| 格式符  | 说明 | 参数类型 |
|---------|------|----------|
| `z`     | 零字节结尾字符串（C 风格） | `String` |
| `s[n]`  | `n` 字节无符号整数长度前缀的字符串（默认 4） | `String` |
| `c[n]`  | 定长字符数组，固定 `n` 字节（必须指定 `n`） | `String` |
| `p`     | 变长前缀字符串（varint 编码长度） | `String` |

### 字节数组类型

| 格式符  | 说明 | 参数类型 |
|---------|------|----------|
| `a[n]`  | 定长字节数组，固定 `n` 字节（必须指定 `n`） | `ByteArray` |
| `A[n]`  | `n` 字节无符号整数长度前缀的字节数组（默认 4） | `ByteArray` |
| `A`     | 变长前缀字节数组（varint 编码长度） | `ByteArray` |

## 使用示例

### 布尔值与位数组

```kotlin
// 单个布尔值
val data = BinPack.pack("b b", true, false)
val (values, _) = BinPack.unpack("b b", data)
// values = [true, false]

// 定长布尔位数组
val bits = BinPack.pack("b8", booleanArrayOf(true, false, true, true, false, false, false, true))
// 编码为 1 字节: 0x8D

// 变长前缀布尔位数组
val dynamic = BinPack.pack("B", booleanArrayOf(true, true, false))
```

### 整数与浮点数

```kotlin
// 小端序 32 位有符号整数 + 64 位浮点数
val data = BinPack.pack("<i32 f64", 42L, 3.14)

// 大端序
val bigEndian = BinPack.pack(">i32", 0x01020304L)

// 半精度浮点数
val half = BinPack.pack("<f16", 1.5)
```

### 变长编码

```kotlin
// 有符号变长整数 (ZigZag + LEB128)
val signed = BinPack.pack("v", -100L)

// 无符号变长整数 (LEB128)
val unsigned = BinPack.pack("V", 10000L)

// 变长前缀浮点数 (自动选择最紧凑编码)
val zero = BinPack.pack("g", 0.0)     // 1 字节
val half = BinPack.pack("g", 1.5)     // 3 字节 (float16)
val pi   = BinPack.pack("g", 3.14)    // 5 字节 (float32)
```

### 字符串

```kotlin
// 零结尾字符串
val zStr = BinPack.pack("z", "hello")

// 4 字节前缀字符串
val sStr = BinPack.pack("<s4", "你好世界")

// 变长前缀字符串
val pStr = BinPack.pack("p", "varint prefixed")

// 定长字符数组
val cStr = BinPack.pack("c16", "fixed")
```

### 复合格式与连续解包

```kotlin
// 复合数据包
val flags = booleanArrayOf(true, false, true, false, false, false, false, true)
val extra = byteArrayOf(0x01, 0x02, 0x03)
val packet = BinPack.pack("<i8 b8 p f32 A", 1L, flags, "Player1", 99.5, extra)

val (values, _) = BinPack.unpack("<i8 b8 p f32 A", packet)
// values[0] = 1L (版本)
// values[1] = BooleanArray (标志位)
// values[2] = "Player1" (名称)
// values[3] = 99.5 (分数)
// values[4] = ByteArray (附加数据)

// 连续解包：利用 nextOffset 从同一缓冲区中依次解包不同格式的数据
val part1 = BinPack.pack("<i32 p", 100L, "first")
val part2 = BinPack.pack("<f32 b", 1.0, true)
val combined = part1 + part2

val r1 = BinPack.unpack("<i32 p", combined, 0)
val r2 = BinPack.unpack("<f32 b", combined, r1.nextOffset)
```

### 大小预测

```kotlin
// 纯固定大小格式 → exact 非 null
val pred = BinPack.predictSize("<i32 f64 b")
println(pred.exact) // 13

// 含变长字段 → exact 为 null
val pred2 = BinPack.predictSize("<i32 v p")
println(pred2.fixedBytes)      // 4
println(pred2.variableFields)  // 2
println(pred2.exact)           // null

// 精确计算（需要实际参数）
val size = BinPack.computeSize("<i32 v p", 0L, 42L, "hello")
```

## 项目结构

```
BinPack/
├── src/
│   ├── BinPack.kt    # 核心库：格式解析、打包、解包、大小预测
│   └── Main.kt       # 示例程序：演示所有格式符的用法
├── BinPack.iml        # IntelliJ IDEA 模块配置
└── .gitignore
```

## 许可证

本项目遵循 MIT 许可证。
