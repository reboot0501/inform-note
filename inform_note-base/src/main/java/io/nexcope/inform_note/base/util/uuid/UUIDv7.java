package io.nexcope.inform_note.base.util.uuid;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

public class UUIDv7 {
   private static final SecureRandom random = new SecureRandom();

   public static String random() {
      long timestamp = Instant.now().toEpochMilli();
      long mostSigBits = timestamp << 16 & -65536L;
      mostSigBits |= 28672L;
      byte[] randBytes = new byte[10];
      random.nextBytes(randBytes);
      long leastSigBits = ByteBuffer.wrap(randBytes).getLong() & 4611686018427387903L;
      leastSigBits |= Long.MIN_VALUE;
      return (new UUID(mostSigBits, leastSigBits)).toString();
   }

   public static void main(String[] args) throws InterruptedException {
      for(int i = 0; i < 100; ++i) {
         String uuidV7 = random();
         System.out.printf("UUID v7(%s), %02d : %s%n", System.currentTimeMillis(), i, uuidV7);
         Thread.sleep(1L);
      }

   }

   private UUIDv7() {
   }
}
