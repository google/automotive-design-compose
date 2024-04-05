
# Cheatsheet

| Protobuf Type | Rust Type | VSCode snippet |
| --- | --- | --- |
| `message` | `pub struct {}` | `msg` |
| `map` | `HashMap<String, T>` | `fm` |
| `repeated` | `Vec<T>` | <none> |
| `optional` | `Option<T>` | <none> |
| `oneof` | `enum` | `foo` |
| `enum` | `enum` | `en` |
| `service` | `struct` | `sv` |
| `double` | `f64` | `fdo` |
| `float` | `f32` |`ffl` |
| `int32` | `i32` | `fi32` |
| `int64` | `i64` | `fi64` |
| `uint32` | `u32` | `fu32` |
| `uint64` | `u64` | `fu64` |
| `sint32` | `i32` | `fs32` |
| `sint64` | `i64` | `fs64` |
| `fixed32` | `u32` | `ff32` |
| `fixed64` | `u64` | `ff64` |
| `sfixed32` | `i32` | `fsf32` |
| `sfixed64` | `i64` | `fsf64` |
| `bool` | `bool` | `fbo` |
| `string` | `String` | `fst` |
| `bytes` | `Vec<u8>` | `fby` |

# Rust

Using [prost](https://github.com/tokio-rs/prost):

## Fields

Fields in Protobuf messages are translated into Rust as public struct fields of the
corresponding type.

## Scalar Values

Scalar value types are converted as follows:

| Protobuf Type | Rust Type |
| --- | --- |
| `double` | `f64` |
| `float` | `f32` |
| `int32` | `i32` |
| `int64` | `i64` |
| `uint32` | `u32` |
| `uint64` | `u64` |
| `sint32` | `i32` |
| `sint64` | `i64` |
| `fixed32` | `u32` |
| `fixed64` | `u64` |
| `sfixed32` | `i32` |
| `sfixed64` | `i64` |
| `bool` | `bool` |
| `string` | `String` |
| `bytes` | `Vec<u8>` |

# IDE Support

Recommend the [vscode-proto3](https://marketplace.visualstudio.com/items?itemName=zxh404.vscode-proto3) extension. It provides some code snippets to make it easier to write protobuf code:

| prefix | body                                           |
| ------ | ---------------------------------------------- |
| sp2    | `syntax = "proto2";`                           |
| sp3    | `syntax = "proto3";`                           |
| pkg    | `package package.name;`                        |
| imp    | `import "path/to/other/protos.proto";`         |
| ojp    | `option java_package = "java.package.name";`   |
| ojoc   | `option java_outer_classname = "ClassName";`   |
| o4s    | `option optimize_for = SPEED;`                 |
| o4cs   | `option optimize_for = CODE_SIZE;`             |
| o4lr   | `option optimize_for = LITE_RUNTIME;`          |
| odep   | `option deprecated = true;`                    |
| oaa    | `option allow_alias = true;`                   |
| msg    | `message MessageName {}`                       |
| fbo    | `bool field_name = tag;`                       |
| fi32   | `int32 field_name = tag;`                      |
| fi64   | `int64 field_name = tag;`                      |
| fu32   | `uint32 field_name = tag;`                     |
| fu64   | `uint64 field_name = tag;`                     |
| fs32   | `sint32 field_name = tag;`                     |
| fs64   | `sint64 field_name = tag;`                     |
| ff32   | `fixed32 field_name = tag;`                    |
| ff64   | `fixed64 field_name = tag;`                    |
| fsf32  | `sfixed32 field_name = tag;`                   |
| fsf64  | `sfixed64 field_name = tag;`                   |
| ffl    | `float field_name = tag;`                      |
| fdo    | `double field_name = tag;`                     |
| fst    | `string field_name = tag;`                     |
| fby    | `bytes field_name = tag;`                      |
| fm     | `map<key, val> field_name = tag;`              |
| foo    | `oneof name {}`                                |
| en     | `enum EnumName {}`                             |
| sv     | `service ServiceName {}`                       |
| rpc    | `rpc MethodName (Request) returns (Response);` |
