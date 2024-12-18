// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.embedding.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The resulting Flutter key events generated by {@link KeyEmbedderResponder}, and are sent through
 * the messenger after being marshalled with {@link #toBytes()}.
 *
 * <p>This class is the Java adaption of {@code KeyData} and {@code KeyDataPacket} in the C engine.
 * Changes made to either side must also be made to the other.
 *
 * <p>Each {@link KeyData} corresponds to a {@code ui.KeyData} in the framework.
 */
public class KeyData {
  private static final String TAG = "KeyData";

  /**
   * The channel that key data should be sent through.
   *
   * <p>Must be kept in sync with kFlutterKeyDataChannel in embedder.cc
   */
  public static final String CHANNEL = "flutter/keydata";

  // The number of fields except for `character`.
  // If this value changes, update the code in the following files:
  //
  //  * key_data.h (kKeyDataFieldCount)
  //  * platform_dispatcher.dart (_kKeyDataFieldCount)
  private static final int FIELD_COUNT = 6;
  private static final int BYTES_PER_FIELD = 8;

  /** The action type of the key data. */
  public enum Type {
    kDown(0),
    kUp(1),
    kRepeat(2);

    private long value;

    private Type(long value) {
      this.value = value;
    }

    public long getValue() {
      return value;
    }

    static Type fromLong(long value) {
      switch ((int) value) {
        case 0:
          return kDown;
        case 1:
          return kUp;
        case 2:
          return kRepeat;
        default:
          throw new AssertionError("Unexpected Type value");
      }
    }
  }

  /** The device type of the key data. */
  public enum DeviceType {
    kKeyboard(0),
    kDirectionalPad(1),
    kGamepad(2),
    kJoystick(3),
    kHdmi(4);

    private final long value;

    private DeviceType(long value) {
      this.value = value;
    }

    public long getValue() {
      return value;
    }

    static DeviceType fromLong(long value) {
      switch ((int) value) {
        case 0:
          return kKeyboard;
        case 1:
          return kDirectionalPad;
        case 2:
          return kGamepad;
        case 3:
          return kJoystick;
        case 4:
          return kHdmi;
        default:
          throw new AssertionError("Unexpected DeviceType value");
      }
    }
  }

  /** Creates an empty {@link KeyData}. */
  public KeyData() {}

  /**
   * Unmarshal fields from a buffer.
   *
   * <p>For the binary format, see {@code lib/ui/window/key_data_packet.h}.
   */
  public KeyData(@NonNull ByteBuffer buffer) {
    final long charSize = buffer.getLong();
    this.timestamp = buffer.getLong();
    this.type = Type.fromLong(buffer.getLong());
    this.physicalKey = buffer.getLong();
    this.logicalKey = buffer.getLong();
    this.synthesized = buffer.getLong() != 0;
    this.deviceType = DeviceType.fromLong(buffer.getLong());

    if (buffer.remaining() != charSize) {
      throw new AssertionError(
          String.format(
              "Unexpected char length: charSize is %d while buffer has position %d, capacity %d, limit %d",
              charSize, buffer.position(), buffer.capacity(), buffer.limit()));
    }
    this.character = null;
    if (charSize != 0) {
      final byte[] strBytes = new byte[(int) charSize];
      buffer.get(strBytes, 0, (int) charSize);
      try {
        this.character = new String(strBytes, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError("UTF-8 unsupported");
      }
    }
  }

  long timestamp;
  Type type;
  long physicalKey;
  long logicalKey;
  boolean synthesized;
  DeviceType deviceType;

  /** The character of this key data encoded in UTF-8. */
  @Nullable String character;

  /**
   * Marshal the key data to a new byte buffer.
   *
   * <p>For the binary format, see {@code lib/ui/window/key_data_packet.h}.
   *
   * @return the marshalled bytes.
   */
  ByteBuffer toBytes() {
    byte[] charBytes;
    try {
      charBytes = character == null ? null : character.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("UTF-8 not supported");
    }
    final int charSize = charBytes == null ? 0 : charBytes.length;
    final ByteBuffer packet =
        ByteBuffer.allocateDirect((1 + FIELD_COUNT) * BYTES_PER_FIELD + charSize);
    packet.order(ByteOrder.LITTLE_ENDIAN);

    packet.putLong(charSize);
    packet.putLong(timestamp);
    packet.putLong(type.getValue());
    packet.putLong(physicalKey);
    packet.putLong(logicalKey);
    packet.putLong(synthesized ? 1L : 0L);
    packet.putLong(deviceType.getValue());
    if (charBytes != null) {
      packet.put(charBytes);
    }

    return packet;
  }
}