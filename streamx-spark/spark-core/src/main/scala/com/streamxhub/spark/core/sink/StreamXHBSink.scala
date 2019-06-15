package com.streamxhub.spark.core.sink

import java.util.Properties

import com.streamxhub.spark.core.support.hbase.HBaseClient
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  *
  */
class StreamXHBSink(@transient override val sc: SparkContext,
                 val initParams: Map[String, String] = Map.empty[String, String]) extends Sink[StreamXMutation] {


  override val paramPrefix: String = "spark.sink.hbase."

  private lazy val prop = {
    val p = new Properties()
    p.putAll(param.map { case (k, v) => s"hbase.$k" -> v } ++ initParams)
    p
  }

  private val tableName = prop.getProperty("hbase.table")
  private val commitBatch = prop.getProperty("hbase.commit.batch", "1000").toInt

  private def getConnect: Connection = {
    val conf = HBaseConfiguration.create
    prop.foreach { case (k, v) => conf.set(k, v) }
    HBaseClient.connect(conf)
  }

  private def getMutator: BufferedMutator = {
    val connection = getConnect
    val bufferedMutatorParams = new BufferedMutatorParams(TableName.valueOf(tableName))
    connection.getBufferedMutator(bufferedMutatorParams)
  }

  private def getTable: Table = {
    val connection = getConnect
    connection.getTable(TableName.valueOf(tableName))
  }


  /**
    * 输出
    *
    * @param rdd  spark.RDD
    * @param time spark.streaming.Time
    */
  override def output(rdd: RDD[StreamXMutation], time: Time): Unit = {


    rdd.foreachPartition { iter =>
      val mutator = getMutator
      val table = getTable
      val list = new mutable.ArrayBuffer[Mutation]()

      iter.foreach {
        case StreamXMutation(ActionType.Put, put) => mutator.mutate(put)
        case StreamXMutation(ActionType.Delete, del) => list += del
        case StreamXMutation(ActionType.Increment, inc) => list += inc
        case StreamXMutation(ActionType.Appent, apt) => list += apt
      }

      batch(list: _*)
      mutator.flush()
      mutator.close()
      table.close()

      /**
        * 批量插入
        *
        * @param actions
        */
      def batch(actions: Mutation*): Unit = {
        if (actions.nonEmpty) {
          val start = System.currentTimeMillis()
          val (head, tail) = actions.splitAt(commitBatch)
          table.batch(head, new Array[AnyRef](head.length))

          println(s"batch ${head.size} use ${System.currentTimeMillis() - start} MS")

          batch(tail.toList: _*)
        }
      }
    }
  }
}

object ActionType extends Enumeration {
  type ActionType = Value
  val Put, Delete, Increment, Appent = Value
}

import ActionType._

case class StreamXMutation(actionType: ActionType, mutation: Mutation)