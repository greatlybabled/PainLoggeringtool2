package com.example.painlogger;

import android.util.Log;
import com.example.painlogger.data.ReminderEntity;
import com.example.painlogger.data.model.ReminderConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReminderMapper {
        private static final String TAG = "ReminderMapper";
        private static final Gson gson = new GsonBuilder()
                .registerTypeAdapter(ReminderConfig.class, new ReminderConfigTypeAdapter())
                .create();

        public static Reminder toDomain(ReminderEntity entity) {
                if (entity == null) {
                        return null;
                }

                Log.d(TAG, "Converting ReminderEntity to domain model. ID: " + entity.getId());

                UUID id;
                try {
                        id = entity.getId();
                } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Invalid UUID format: " + entity.getId(), e);
                        id = UUID.randomUUID();
                }

                ReminderCategory category;
                try {
                        category = ReminderCategory.valueOf(entity.getCategory());
                } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Invalid category: " + entity.getCategory(), e);
                        category = ReminderCategory.GENERAL;
                }

                ReminderType type;
                try {
                        type = ReminderType.valueOf(entity.getType().name());
                } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Invalid type: " + entity.getType(), e);
                        type = ReminderType.SPECIFIC_TIME;
                }

                Set<Integer> activeDays = entity.getActiveDays(); // Get Set<Integer> directly

                ReminderConfig config = null;
                String configJson = entity.getConfig();
                if (configJson != null && !configJson.isEmpty()) {
                        try {
                                config = gson.fromJson(configJson, ReminderConfig.class);
                        } catch (Exception e) {
                                Log.e(TAG, "Error parsing config: " + configJson, e);
                        }
                }

                return new Reminder(
                        id,
                        entity.getTitle(),
                        category,
                        type,
                        entity.isEnabled(),
                        activeDays,
                        config
                );
        }


        public static ReminderEntity toEntity(Reminder reminder) {
                if (reminder == null) {
                        return null;
                }

                String configJson = null;
                ReminderConfig config = reminder.getConfig();
                if (config != null) {
                        try {
                                configJson = gson.toJson(config, ReminderConfig.class);
                        } catch (Exception e) {
                                Log.e(TAG, "Error serializing config", e);
                        }
                }

                return new ReminderEntity(
                        reminder.getId(),
                        reminder.getTitle(),
                        reminder.getCategory().name(),
                        convertToEntityType(reminder.getType()),
                        reminder.isEnabled(),
                        reminder.getActiveDays(),  // Pass Set<Integer> directly
                        configJson
                );
        }


        private static com.example.painlogger.data.ReminderType convertToEntityType(ReminderType type) {
                return com.example.painlogger.data.ReminderType.valueOf(type.name());
        }

        private static class ReminderConfigTypeAdapter extends TypeAdapter<ReminderConfig> {
                @Override
                public void write(JsonWriter out, ReminderConfig value) throws IOException {
                        if (value == null) {
                                out.nullValue();
                                return;
                        }

                        out.beginObject();
                        if (value instanceof ReminderConfig.SpecificTimeConfig) {
                                ReminderConfig.SpecificTimeConfig specificConfig = (ReminderConfig.SpecificTimeConfig) value;
                                out.name("type").value("specific_time");
                                out.name("times");
                                out.beginArray();
                                for (ReminderConfig.Time time : specificConfig.getTimes()) {
                                        out.beginObject();
                                        out.name("hour").value(time.getHour());
                                        out.name("minute").value(time.getMinute());
                                        out.endObject();
                                }
                                out.endArray();
                        } else if (value instanceof ReminderConfig.IntervalConfig) {
                                ReminderConfig.IntervalConfig intervalConfig = (ReminderConfig.IntervalConfig) value;
                                out.name("type").value("interval");
                                out.name("intervalHours").value(intervalConfig.getIntervalHours());
                                out.name("intervalMinutes").value(intervalConfig.getIntervalMinutes());
                        }
                        out.endObject();
                }

                @Override
                public ReminderConfig read(JsonReader in) throws IOException {
                        if (in.peek() == null) {
                                in.nextNull();
                                return null;
                        }

                        in.beginObject();
                        String type = null;
                        List<ReminderConfig.Time> times = new ArrayList<>();
                        int intervalHours = 0;
                        int intervalMinutes = 0;

                        while (in.hasNext()) {
                                String name = in.nextName();
                                switch (name) {
                                        case "type":
                                                type = in.nextString();
                                                break;
                                        case "times":
                                                in.beginArray();
                                                while (in.hasNext()) {
                                                        in.beginObject();
                                                        int hour = 0;
                                                        int minute = 0;
                                                        while (in.hasNext()) {
                                                                switch (in.nextName()) {
                                                                        case "hour":
                                                                                hour = in.nextInt();
                                                                                break;
                                                                        case "minute":
                                                                                minute = in.nextInt();
                                                                                break;
                                                                        default:
                                                                                in.skipValue();
                                                                                break;
                                                                }
                                                        }
                                                        times.add(new ReminderConfig.Time(hour, minute));
                                                        in.endObject();
                                                }
                                                in.endArray();
                                                break;
                                        case "intervalHours":
                                                intervalHours = in.nextInt();
                                                break;
                                        case "intervalMinutes":
                                                intervalMinutes = in.nextInt();
                                                break;
                                        default:
                                                in.skipValue();
                                                break;
                                }
                        }
                        in.endObject();

                        if ("specific_time".equals(type)) {
                                return new ReminderConfig.SpecificTimeConfig(
                                        "reminder_" + UUID.randomUUID().toString(),
                                        "Specific Time Reminder",
                                        "It's time for your reminder",
                                        times
                                );
                        } else if ("interval".equals(type)) {
                                return new ReminderConfig.IntervalConfig(
                                        "reminder_" + UUID.randomUUID().toString(),
                                        "Interval Reminder",
                                        "It's time for your reminder",
                                        intervalHours,
                                        intervalMinutes
                                );
                        }
                        return null;
                }
        }
}