/*
 *  Copyright 2021 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.benchmark

import smithy4s._
import smithy.api.TimestampFormat
import smithy4s.Timestamp

object Payload {

  val loremIpsumString =
    """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus sed
        |elementum lacus. Vestibulum id facilisis neque. Sed sagittis libero nec
        |metus hendrerit, at iaculis leo placerat. Duis non fermentum justo, eget
        |lobortis odio. Vivamus facilisis diam non nunc suscipit, nec rhoncus ex
        |bibendum. Orci varius natoque penatibus et magnis dis parturient montes,
        |ridiculus mus. Sed pretium diam nec massa dignissim tempus. Quisque quis
        |commodo tortor""".stripMargin.trim()

  val loremIpsum = ByteArray(loremIpsumString.getBytes())

  val timestamp = Timestamp
    .parse("2021-03-14T10:11:04Z", TimestampFormat.DATE_TIME)
    .getOrElse(throw new Exception("bad timestamp"))
  val input = S3Object(
    id = "bla bla blah",
    owner = "ncordon",
    data = loremIpsum,
    attributes = Attributes(
      queryable = Some(false),
      queryableLastChange = Some(timestamp),
      blockPublicAccess = Some(false),
      permissions = Some(
        List(
          Permission(
            read = Some(false),
            write = Some(true),
            directory = Some(false)
          )
        )
      ),
      backedUp = Some(true),
      tags = Some(
        List("disney", "this-is-a-test", "running-out-of-names")
      ),
      encryption = Some(
        Encryption(
          user = Some("john doe"),
          date = Some(timestamp),
          metadata = Some(
            EncryptionMetadata(
              system = Some("IVxga6IRhXlmw4D/MD0="),
              partial = Some(false),
              credentials = Some(Creds(user = Some("john"), key = Some("doe")))
            )
          )
        )
      ),
      metadata = Some(
        List(
          Metadata(
            contentType = Some("Content-Type-Text"),
            lastModified = Some(timestamp),
            checkSum = Some(
              """DCeldzpc4Dkgmg1zbNXkKS+Aaqgz5HHOx3P9osVNk2M2HNwe1fy/Gie8lAsLpPm8
            |NIF9A1z94w2u7N5LQA20tkoENmj9AJKcMQPeeIYO52xnskdbgCBTTLt2V4HFEh2A
            |+Y1TmU7l9qTTuaOHUsGk5RXqY1YMrpl8HZNg""".stripMargin
            ),
            pendingDeletion = Some(true),
            etag = Some(
              """EWMRO34ILrHqRXeZpdw0JGcMAJVI1b5VCU5bkdcy02I+e1mKJcL2XJzAdHTdb9kG
            |4lZ2UcjyHTYGyXbjm2ZWWv3G/PkGSqKI4owMDS8HqEL0HzZ1FXCALbUm/LqhmAlZ
            |nSV1H2aAzwIlhF3zNmXa+AmSihrJjpirBiGU""".stripMargin
            )
          ),
          Metadata(
            contentType = Some("Content-Type-Text"),
            lastModified = Some(timestamp),
            checkSum = Some(
              """E3w8UjvE/KI+nH/HtHrkZIyN+I1LEgLYVGOsd6kgQ36PTwL9MOQE40PX42ts3uCs
                |02lHnnZY1TWQrsPize8PL/oq77UOoXekHFlfptGuPf2ssH8FP7rrMwYxdckDUUdA
                |MtFe8sQzxvsQpfP8OGiBFx0gDtwZ3/Rdxrs5wl+6377AlTQTsB0Qkuci5Avk4Sj7
                |6hFk8xV5b83nc4wqz+aaQ9Xx2bGD2z2bETMxO9VTvPLBT+pT""".stripMargin
            ),
            pendingDeletion = Some(true),
            etag = Some(
              """lQ50wveth7+avq5iRpmhMDTwChqdhQt6zbDjAGCh+VzlgKhgk81X7BJXnCR1TZFD
                |XjZGbCEPCCqKu0CUrb4ges0QedGxaQ==""".stripMargin
            )
          ),
          Metadata(
            contentType = Some("Content-Type-Text"),
            lastModified = Some(timestamp),
            checkSum = Some(
              """9ZEFEogie/8RusAtOfMjpXPTjNnY1oJ0Mp6kBuiuxMMpQQji9y9wPogjHYSguTTZ
                |kYUvKsjVymQ1rmiQRBe5it/LyIJIDTXuBEzZu8txlMY=""".stripMargin
            ),
            pendingDeletion = Some(true),
            etag = Some(
              """EWMRO34ILrHqRXeZpdw0JGcMAJVI1b5VCU5bkdcy02I+e1mKJcL2XJzAdHTdb9kG
            |4lZ2UcjyHTYGyXbjm2ZWWv3GLqhmAlZnSV1H2aAzwIlhF3zNmXa+AmSihrJjpirBiGU""".stripMargin
            )
          )
        )
      ),
      public = false,
      user = "unknown",
      size = 13453124L,
      creationDate = timestamp,
      region = "us-east-1"
    )
  )
}
