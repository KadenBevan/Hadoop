import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/*
 * This program is designed to find the count of every word in an input file set.
 */

public class TopTenTemp extends Configured implements Tool {
		
 	public static class MyMapper extends Mapper<LongWritable, Text, DoubleWritable, Text>
 	{	
 		private static int STATION_ID	= 0;
		private static int ZIPCODE		= 1;
		private static int LAT			= 2;
		private static int LONG			= 3;
		private static int TEMP			= 4;
		private static int PERCIP		= 5;
		private static int HUMID		= 6;
		private static int YEAR			= 7;
		private static int MONTH		= 8;
		
 		public void map(LongWritable inputKey, Text inputValue, Context context) throws IOException, InterruptedException
 		{
 			//StationID | Zipcode |  Lat      |  Lon       |  Temp   | Percip | Humid | Year | Month 
 			String[] element = new String[9];
 			StringTokenizer st = new StringTokenizer(inputValue.toString());
 			
 			for(int i = 0; i < element.length; i++)
 			{
	 			if(st.hasMoreTokens())
	 			{
	 				element[i] = st.nextToken();
	 				if(i == 0 && element[i].equals("StationID"))
	 					return;
	 			}
				else 
					return;
 			} //end for
 			
 			DoubleWritable key = new DoubleWritable( (-1.0) * Double.parseDouble(element[TEMP]));
 			Text value = new Text(new String(element[ZIPCODE]+" "+element[YEAR]+" "+element[MONTH]));
 			
 			context.write(key, value);
 			
 		}//end map
 		
	}//end MyMapper

	public static class MyReducer extends Reducer<DoubleWritable, Text, Text, DoubleWritable>
	{
		static int count;
		
		public void reduce(DoubleWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException
		{
			double temp = ((-1.0) * key.get());
			
			String val = null;
			
			for (Text value : values)
			{
				val = value.toString();
			}
			if (count < 10)
			{
				context.write(new Text(val), new DoubleWritable(temp));
				count++;
			}
		}//end reduce
		
	}//end MyReducer
 	
	public static void main(String[] args) throws Exception
	{
		int res = ToolRunner.run(new Configuration(), new TopTenTemp(), args);
		System.exit(res);
	}
	
	public int run(String[] args) throws Exception 
	{
		
		if(args.length != 3)
		{
			System.out.println("Usage: bin/hadoop jar MapReduceSample.jar WordCount <input directory> <ouput directory> <number of reduces>");
			System.out.println("args length incorrect, length: " + args.length);
			return -1;
		}
		int numReduces;
		
		Path inputPath = new Path(args[0]);
		Path outputPath = new Path(args[1]);
		
		try
		{
			numReduces 	= new Integer(args[2]);
			System.out.println("number reducers set to: " + numReduces);
		}
		catch(NumberFormatException e)
		{
			System.out.println("Usage: bin/hadoop jar MapReduceSample.jar WordCount <input directory> <ouput directory> <number of reduces>");
			System.out.println("Error: number of reduces not a type integer");
			return -1;
		}
		
		
		Configuration conf = new Configuration();
		
		//FileSystem fs = FileSystem.get(conf);
		
//		if(!fs.exists(inputPath))
//		{
//			System.out.println("Usage: bin/hadoop jar MapReduceSample.jar WordCount <input directory> <ouput directory> <number of reduces>");
//			System.out.println("Error: Input Directory Does Not Exist");
//			System.out.println("Invalid input Path: " + inputPath.toString());
//			return -1;
//		}
//		
//		if(fs.exists(outputPath))
//		{
//			System.out.println("Usage: bin/hadoop jar MapReduceSample.jar WordCount <input directory> <ouput directory> <number of reduces>");
//			System.out.println("Error: Output Directory Already Exists");
//			System.out.println("Please delete or specifiy different output directory");
//			return -1;
//		}
		
		
		conf.set("mapred.child.java.opts", "-Xmx512M");
		conf.setBoolean("mapred.output.compress", false);
		conf.set("mapred.task.timeout", "8000000");
		
		Job job = new Job(conf, "Word Count");
		
		job.setNumReduceTasks(numReduces);
		job.setJarByClass(TopTenTemp.class);
		
		//sets mapper class
		job.setMapperClass(MyMapper.class);
		
		//sets map output key/value types
	    job.setMapOutputKeyClass(DoubleWritable.class);
	    job.setMapOutputValueClass(Text.class);
	    
		//Set Reducer class
	    job.setReducerClass(MyReducer.class);
	    
	    // specify output types
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);	
		
		//sets Input format
	    job.setInputFormatClass(TextInputFormat.class);
	    
	    // specify input and output DIRECTORIES (not files)
	    FileInputFormat.addInputPath(job, inputPath);
		FileOutputFormat.setOutputPath(job, outputPath);
		
		job.waitForCompletion(true);
		
		return 0;
	}
 	
}