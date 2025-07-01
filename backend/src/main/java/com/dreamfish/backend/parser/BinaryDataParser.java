package com.dreamfish.backend.parser;

import com.dreamfish.backend.parser.annotation.BinaryField;
import com.dreamfish.backend.parser.annotation.BinaryRequestBody;
import com.dreamfish.backend.parser.exception.ProtocolException;
import com.dreamfish.backend.parser.exception.ProtocolParseException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 二进制数据解析器:这个类的逻辑写的太失败,就后期加了一个分组功能,代码就没法看,哈哈
 * @date 2025/4/3 16:44
 */
@Slf4j
public class BinaryDataParser {


    // 最大嵌套深度
    private static final int MAX_NESTED_DEPTH = 5;

    //默认字节序
    private static final BinaryRequestBody.ByteOrder DEFAULT_BYTE_ORDER = BinaryRequestBody.ByteOrder.BIG_ENDIAN;

    // 存储解析过的类和字段
    private final Map<Class<?>, List<Field>> fieldCache = new ConcurrentHashMap<>();
    // private final Map<Class<?>, List<Field>> fieldCache = new HashMap<>();


    /**
     * 解析二进制数据到Java对象
     */
    public <T> T parse(byte[] data, Class<T> clazz, Class<?> group, BinaryRequestBody.ByteOrder order) throws ProtocolException {
        if (data == null || data.length == 0) {
            throw new ProtocolParseException("Request body data is null or empty!");
        }
        if (clazz == null) {
            throw new ProtocolParseException("Target type is null!");
        }
        // 获取字节序
        ByteOrder byteOrder;
        order = order == null ? DEFAULT_BYTE_ORDER : order;
        // 获取字节序
        if (order == com.dreamfish.backend.parser.annotation.BinaryRequestBody.ByteOrder.BIG_ENDIAN) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        } else {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        }
        //基本数据类型的处理: if (clazz.isPrimitive() || clazz == String.class) { pass }

        //从缓存中获取字段列表
        buildFieldCache(clazz);

        // 将字节数组转换为ByteBuffer并设置字节序
        ByteBuffer buffer = ByteBuffer.wrap(data).order(byteOrder);
        return parseBuffer(buffer, clazz, group, byteOrder, 1);
    }

    public void buildFieldCache(Class<?> clazz) {
        // 如果缓存中没有，则进行验证和解析
        if (fieldCache.getOrDefault(clazz, null) == null) {
            // 如果缓存中没有，则进行验证和解析
            BinaryFieldValidator validator = new BinaryFieldValidator();
            validator.validateProtocolClass(clazz, 1);
            // 存储验证过程中解析到的: 当前类及其嵌套类的有序的含有BinaryField注解的字段列表
            fieldCache.putAll(validator.getOrderFieldsList());
        }
    }

    /**
     * 递归解析方法（处理嵌套对象）
     */
    private <T> T parseBuffer(ByteBuffer buffer, Class<T> clazz, Class<?> group, ByteOrder byteOrder, int depth) {
        if (depth > MAX_NESTED_DEPTH) {
            throw new ProtocolParseException("Exceeded maximum nested depth: " + MAX_NESTED_DEPTH);
        }
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : fieldCache.get(clazz)) {
                // 获取 BinaryField 注解
                BinaryField annotation = field.getAnnotation(BinaryField.class);
                // 获取分组信息
                for (Class<?> g : annotation.groups()) {
                    // 当前字段在选中的分组中才能解析
                    if (g == group) {
                        parseField(buffer, instance, field, group, byteOrder, depth);
                        // 解析完当前字段后,跳出循环
                        break;
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            throw new ProtocolParseException("Failed to parse object" +
                    " of type " + clazz.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 解析单个字段
     */
    private void parseField(ByteBuffer buffer, Object target, Field field, Class<?> group, ByteOrder byteOrder, int depth) {
        BinaryField annotation = field.getAnnotation(BinaryField.class);
        try {
            field.setAccessible(true);
            field.set(target, parseFieldValue(buffer, field, annotation, group, byteOrder, depth));
        } catch (BufferUnderflowException e) {
            throw new ProtocolParseException("Buffer underflow at field: " + field.getName());
        } catch (Exception e) {
            throw new ProtocolParseException(
                    String.format(
                            "Field parse error: %s (type: %s)", field.getName(), field.getType().getSimpleName()),
                    e
            );
        }
    }

    /**
     * 字段值解析核心逻辑
     */
    private Object parseFieldValue(ByteBuffer buffer, Field field, BinaryField annotation, Class<?> group, ByteOrder byteOrder, int depth) {
        // 处理嵌套对象
        if (annotation.nested()) {
            // 获取属性中定义的嵌套类类型
            Class<?> nestedType = field.getType();
            //构建该类型的字段缓存:已经检查过了,并将嵌套类的含注解字段有序列表存储到了缓存中
            // buildFieldCache(nestedType);

            // 对原始缓冲区进行切片，创建一个新的 ByteBuffer
            //  slice() 方法创建一个新的 ByteBuffer,只含有原始缓冲区的剩余部分
            ByteBuffer nestedBuffer = buffer.slice().order(byteOrder);

            // 递归解析嵌套对象
            Object o = parseBuffer(nestedBuffer, nestedType, group, byteOrder, depth + 1);

            // 设置原来 buffer 的 position
            buffer.position(buffer.position() + nestedBuffer.position());

            return o;
        }

        // 基本类型解析
        Class<?> fieldType = field.getType();
        int length = getLength(annotation, fieldType);

        //  判断剩余字节数是否足够
        if (buffer.remaining() < length) {
            throw new ProtocolParseException(String.format(
                    "Buffer underflow at field %s (需要 %d 字节，剩余 %d 字节)",
                    field.getName(), length, buffer.remaining()
            ));
        }

        // 在解析关键步骤添加日志
        log.debug("解析字段 {}，类型 {}，长度 {}，当前 position={}/limit={}",
                field.getName(), field.getType(), length, buffer.position(), buffer.limit());

        // 从切片中读取一个 length 长度字节数组
        // 该 get 方法会自动移动指针,无需手动 buffer.position(buffer.position() + length);
        byte[] bytes = new byte[length];
        buffer.get(bytes, 0, length);

        // 根据类型进行转换
        return switch (fieldType.getSimpleName()) {
            case "byte", "Byte" -> UniversalByteConverter.converterToByte(bytes, byteOrder);
            case "boolean", "Boolean" -> UniversalByteConverter.converterToBoolean(bytes, byteOrder);
            case "short", "Short" -> UniversalByteConverter.converterToShort(bytes, byteOrder);
            case "int", "Integer" -> UniversalByteConverter.converterToInt(bytes, byteOrder);
            case "long", "Long" -> UniversalByteConverter.converterToLong(bytes, byteOrder);
            case "float", "Float" -> UniversalByteConverter.converterToFloat(bytes, byteOrder, annotation.scale());
            case "double", "Double" -> UniversalByteConverter.converterToDouble(bytes, byteOrder, annotation.scale());
            //在 BinaryFieldValidator中已经验证过cale 和 length的合法性,直接放心大胆转换
            case "BigDecimal" -> UniversalByteConverter.converterToBigDecimal(bytes, byteOrder, annotation.scale());
            // 在 BinaryFieldValidator中已经验证过编码格式的合法性,这里直接大胆转换
            case "String" -> UniversalByteConverter.converterToString(bytes, Charset.forName(annotation.charset()));
            default -> throw new ProtocolParseException("Unsupported data type: " + fieldType.getSimpleName());
        };
    }

    private static int getLength(BinaryField annotation, Class<?> fieldType) {
        int length = annotation.length();

        //尝试读取 length 字节并根据类型进行解析
        if (length == 0) {
            // 如果长度为-1，则根据类型自动推导长度
            length = switch (fieldType.getSimpleName()) {
                case "boolean", "Boolean" -> 1;
                case "byte", "Byte" -> Byte.BYTES;
                case "short", "Short" -> Short.BYTES;
                case "int", "Integer" -> Integer.BYTES;
                case "long", "Long" -> Long.BYTES;
                case "float", "Float" -> Float.BYTES;
                case "double", "Double" -> Double.BYTES;
                // 其实下面的内容已经在 BinaryFieldValidator中验证过了
                case "String" -> throw new ProtocolParseException("String length must be specified in the annotation");
                case "BigDecimal" ->
                        throw new ProtocolParseException("BigDecimal length must be specified in the annotation");
                default -> throw new ProtocolParseException("Unsupported data type: " + fieldType.getSimpleName());
            };
        }
        return length;
    }

}
