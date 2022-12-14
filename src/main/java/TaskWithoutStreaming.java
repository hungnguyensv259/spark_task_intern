import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.*;

//Thực hiện truy vấn dữ liệu chỉ 1 lần, không phải dạng trực tuyến
public class TaskWithoutStreaming {

    public static void main(String[] args) {
        SparkSession spark = SparkSession
                .builder()
                .appName("spark tasks")
                .getOrCreate();

        String sourceFile = "spark_task_intern/data";
        String resultFolder = "resulttest";

        Dataset<Row> df = spark
                .read()
                .option("mergeSchema", "true")
                .parquet(sourceFile)
                .select("campaign", "cov", "location", "guid", "year", "month", "day")
                .withColumn("date", expr("make_date(year, month, day)"));

//        Số lượng click, view ứng với mỗi campaign
        Dataset<Row> df1 = df.groupBy("date", "campaign", "cov").count();
        Dataset<Row> df2 = df1
                .groupBy("date", "campaign")
                .agg(sum("count").as("sum"));
        df1.createOrReplaceTempView("df1");
        df2.createOrReplaceTempView("df2");
        Dataset<Row> ex1 = spark.sql("select df2.date, df2.campaign, ifnull(df1.count, 0) as view, ifnull(df2.sum-df1.count, sum) as click "
                + "from df1 right join df2 "
                + "on df1.date=df2.date and df1.campaign=df2.campaign and df1.cov=0 "
                + "order by df2.date desc, df2.campaign desc");
        ex1.show();
        ex1
                .write()
                .option("delimiter", ";")
                .option("header", "true")
                .mode(SaveMode.Overwrite)
                .csv(resultFolder + "/resultExercise1");


//        Tìm số lượng click, tỉ lệ view ứng với mỗi campaign theo location
        Dataset<Row> df3 = df.groupBy("date", "location", "campaign", "cov").count();
        Dataset<Row> df4 = df3.groupBy("date", "location", "campaign").agg(sum("count").as("sum"));
        df3.createOrReplaceTempView("df3");
        df4.createOrReplaceTempView("df4");
        Dataset<Row> ex2 = spark.sql("select df4.date, df4.location, df4.campaign, df4.sum, ifnull(df3.count, 0) as view, ifnull(df4.sum-df3.count, sum) as click "
                + "from df3 right join df4 "
                + "on df4.date=df3.date and df3.campaign=df4.campaign and df3.location=df4.location and df3.cov=0 "
                + "order by df4.date desc, df4.location desc, df4.campaign desc");
        ex2.show();
        ex2.write()
                .option("delimiter", ";")
                .option("header", "true")
                .mode(SaveMode.Overwrite)
                .csv(resultFolder + "/resultExercise2");


//        Tìm tỉ lệ click, tỉ lệ view ứng với mỗi campaign theo location
//        ex2.
//                .withColumn("rateView", col("view").divide(col("sum")))
//                .withColumn("rateClick", col("click").divide(col("sum")));
//        ex2.show();


//      Số lượng user truy cập ứng với mỗi campaign
        Dataset<Row> ex3 = df.groupBy("date", "campaign")
                .agg(countDistinct("guid").alias("count")).
                orderBy(col("date").desc(), col("campaign").desc());
        ex3.show();
        ex3
                .write()
                .option("delimiter", ";")
                .option("header", "true")
                .mode(SaveMode.Overwrite)
                .csv(resultFolder + "/resultExercise3");

//      Số lượng user truy cập nhiều hơn một campaign
        Dataset<Row> ex4 = df.groupBy("date", "guid")
                .agg(count("campaign").as("count"))
                .filter("count>1")
                .groupBy("date").agg(count("guid"))
                .orderBy(col("date").desc());
        ex4.show();
        ex4
                .write()
                .option("delimiter", ";")
                .option("header", "true")
                .mode(SaveMode.Overwrite)
                .csv(resultFolder + "/resultExercise4");
    }
}

