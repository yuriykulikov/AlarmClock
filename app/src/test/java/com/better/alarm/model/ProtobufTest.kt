package com.better.alarm.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import org.assertj.core.api.KotlinAssertions.assertThat
import org.junit.Test

class ProtobufTest {
  @OptIn(ExperimentalSerializationApi::class)
  @Test
  fun `proto2 schema matches expectations`() {
    assertThat(ProtoBufSchemaGenerator.generateSchemaText(AlarmValues.serializer().descriptor))
        .isEqualTo(
            """syntax = "proto2";


// serial name 'com.better.alarm.model.AlarmValues'
message AlarmValues {
  // WARNING: a default value decoded when value is missing
  map<int32, AlarmValue> alarms = 1;
}

// serial name 'com.better.alarm.model.AlarmValue'
message AlarmValue {
  // WARNING: a default value decoded when value is missing
  optional int64 nextTime = 1;
  // WARNING: a default value decoded when value is missing
  optional string state = 2;
  // WARNING: a default value decoded when value is missing
  optional int32 id = 3;
  // WARNING: a default value decoded when value is missing
  optional bool isEnabled = 4;
  // WARNING: a default value decoded when value is missing
  optional int32 hour = 5;
  // WARNING: a default value decoded when value is missing
  optional int32 minutes = 6;
  // WARNING: a default value decoded when value is missing
  optional bool isPrealarm = 7;
  // WARNING: a default value decoded when value is missing
  optional Alarmtone alarmtone = 8;
  // WARNING: a default value decoded when value is missing
  optional bool isVibrate = 9;
  // WARNING: a default value decoded when value is missing
  optional string label = 10;
  // WARNING: a default value decoded when value is missing
  optional DaysOfWeek daysOfWeek = 11;
}

// serial name 'com.better.alarm.model.Alarmtone'
message Alarmtone {
  required string type = 1;
  // decoded as message with one of these types:
  //   message Silent, serial name 'Silent'
  //   message Default, serial name 'Default'
  //   message Sound, serial name 'Sound'
  required bytes value = 2;
}

// serial name 'com.better.alarm.model.DaysOfWeek'
message DaysOfWeek {
  required int32 coded = 1;
}

message Silent {
}

message Default {
}

message Sound {
  required string uriString = 1;
}
""")
  }
}
