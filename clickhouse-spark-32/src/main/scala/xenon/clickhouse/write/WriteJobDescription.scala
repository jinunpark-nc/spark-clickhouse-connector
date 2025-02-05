/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xenon.clickhouse.write

import java.time.ZoneId

import org.apache.spark.sql.clickhouse.{ExprUtils, WriteOptions}
import org.apache.spark.sql.clickhouse.TransformUtils.toSparkTransform
import org.apache.spark.sql.connector.expressions.{Expression, SortOrder, Transform}
import org.apache.spark.sql.types.StructType
import xenon.clickhouse.expr.{Expr, OrderExpr}
import xenon.clickhouse.spec._

case class WriteJobDescription(
  queryId: String,
  dataSetSchema: StructType,
  node: NodeSpec,
  tz: ZoneId,
  tableSpec: TableSpec,
  tableEngineSpec: TableEngineSpec,
  cluster: Option[ClusterSpec],
  localTableSpec: Option[TableSpec],
  localTableEngineSpec: Option[TableEngineSpec],
  shardingKey: Option[Expr],
  partitionKey: Option[List[Expr]],
  sortingKey: Option[List[OrderExpr]],
  writeOptions: WriteOptions
) {

  def targetDatabase(convert2Local: Boolean): String = tableEngineSpec match {
    case dist: DistributedEngineSpec if convert2Local => dist.local_db
    case _ => tableSpec.database
  }

  def targetTable(convert2Local: Boolean): String = tableEngineSpec match {
    case dist: DistributedEngineSpec if convert2Local => dist.local_table
    case _ => tableSpec.name
  }

  def sparkShardExpr: Option[Expression] = (tableEngineSpec, shardingKey) match {
    case (_: DistributedEngineSpec, Some(expr)) => Some(toSparkTransform(expr))
    case _ => None
  }

  def sparkParts: Array[Transform] = ExprUtils.toSparkParts(shardingKey, partitionKey)

  def sparkSortOrders: Array[SortOrder] = ExprUtils.toSparkSortOrders(shardingKey, partitionKey, sortingKey)
}
