
import com.amazonaws.dynamodb.ArchivalSummary
import com.amazonaws.dynamodb.BackupArn
import smithy4s.Timestamp

object Main extends App {

  val archivalSummary = new ArchivalSummary(
    // archivalDateTime needs to be rewritten from a com.amazonaws.dynamodb.Date to a simple timestamp
    archivalDateTime = Some(Timestamp.fromEpochSecond(java.time.Instant.now().getEpochSecond)),

    // archivalReason needs to be rewritten from com.amazonaws.dynamodb.ArchivalReason to a String
    archivalReason = Some("This is just a string"),

    // archivalBackupArn remains as a newtype because com.amazonaws.dynamodb.BackupArn
    // has the @length trait applied
    archivalBackupArn = Some(BackupArn("this-is-a-back-up-arn-with-a-minimum-length"))
  )

}
