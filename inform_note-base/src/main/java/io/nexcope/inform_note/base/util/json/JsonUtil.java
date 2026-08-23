package io.nexcope.inform_note.base.util.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JsonUtil {
    //
    private static final ObjectMapper mapper = getObjectMapper();

    public static String toJson(Object target) {
        if (target == null) {
            return "";
        } else {
            String result = "";

            try {
                ObjectWriter writer = mapper.writer().withoutAttribute("logger");
                return writer.writeValueAsString(target);
            } catch (JsonProcessingException var3) {
                var3.printStackTrace();
                return result;
            }
        }
    }

    public static String toPrettyJson(Object target) {
        if (target == null) {
            return "";
        } else {
            String result = "";

            try {
                ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter().withoutAttribute("logger");
                return writer.writeValueAsString(target);
            } catch (JsonProcessingException var3) {
                var3.printStackTrace();
                return result;
            }
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json != null && !json.trim().isEmpty()) {
            if (clazz == String.class) {
                @SuppressWarnings("unchecked")
                T result = (T) json;
                return result;
            } else {
                T result = null;

                try {
                    result = mapper.readValue(json, clazz);
                } catch (JsonProcessingException var4) {
                    var4.printStackTrace();
                }

                return result;
            }
        } else {
            return null;
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json != null && !json.trim().isEmpty()) {
            if (typeReference.getType() == String.class) {
                @SuppressWarnings("unchecked")
                T result = (T) json;
                return result;
            } else {
                T result = null;

                try {
                    result = mapper.readValue(json, typeReference);
                } catch (JsonProcessingException var4) {
                    var4.printStackTrace();
                }

                return result;
            }
        } else {
            return null;
        }
    }

    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (json != null && !json.trim().isEmpty()) {
            List<T> results = new ArrayList();

            try {
                JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, clazz);
                results = (List) mapper.readValue(json, type);
            } catch (JsonProcessingException var4) {
                var4.printStackTrace();
            }

            return (List) results;
        } else {
            return Collections.emptyList();
        }
    }

    public static <T> Set<T> fromJsonSet(String json, Class<T> clazz) {
        if (json != null && !json.trim().isEmpty()) {
            Set<T> results = new HashSet();

            try {
                JavaType type = mapper.getTypeFactory().constructCollectionType(Set.class, clazz);
                results = (Set) mapper.readValue(json, type);
            } catch (JsonProcessingException var4) {
                var4.printStackTrace();
            }

            return (Set) results;
        } else {
            return Collections.emptySet();
        }
    }

    public static String minifyJson(String json) {
        if (json != null && !json.trim().isEmpty()) {
            try {
                Object jsonObject = mapper.readValue(json, Object.class);
                String minified = mapper.writeValueAsString(jsonObject);
                return minified;
            } catch (JsonProcessingException var3) {
                throw new IllegalArgumentException("Cannot convert json to string", var3);
            }
        } else {
            return json;
        }
    }

    public static boolean isValid(String json) {
        try {
            mapper.readTree(json);
            return true;
        } catch (JacksonException var2) {
            return false;
        }
    }

    private static ObjectMapper getObjectMapper() {
        JavaTimeModule timeModule = getTimeModule();
        ObjectMapper mapper = ((JsonMapper.Builder) ((JsonMapper.Builder) ((JsonMapper.Builder) ((JsonMapper.Builder) ((JsonMapper.Builder) ((JsonMapper.Builder) JsonMapper
                .builder().enable(new MapperFeature[] { MapperFeature.PROPAGATE_TRANSIENT_MARKER }))
                .disable(new SerializationFeature[] { SerializationFeature.FAIL_ON_EMPTY_BEANS }))
                .disable(new SerializationFeature[] { SerializationFeature.WRITE_DATES_AS_TIMESTAMPS }))
                .disable(new DeserializationFeature[] { DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES }))
                .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)).addModule(timeModule)).build();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    private static JavaTimeModule getTimeModule() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dttmFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        timeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dttmFormatter));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dttmFormatter));
        return timeModule;
    }

    private JsonUtil() {
    }
}
