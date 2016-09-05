package source;

import indexing.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

class IdAttribute{
	double id;
	double attribute;
	
	public IdAttribute(Double id, Double attribute) {
		this.id = id;
		this.attribute = attribute;
	}
	
	public double getId() {
		return id;
	}

	public double getAttribute() {
		return attribute;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.valueOf(this.id) + " " + String.valueOf(this.attribute);
	}
}


class IdAttributeSort implements Comparator<IdAttribute>{

	@Override
	public int compare(IdAttribute o1, IdAttribute o2) {
		return o1.attribute<o2.attribute?1:o1.attribute>o2.attribute?-1:0;
	}
	
}

class Tuple implements Comparable<Tuple>{
	private Double id,attribute[],threshold;

	public Tuple(Double id, Double[] attribute,Double[] V) {
		super();
		this.id = id;
		this.attribute = attribute;
		computeThreshold(V);
	}
	public Tuple(Double id, Double[] attribute) {
		super();
		this.id = id;
		this.attribute = attribute;
		threshold = 0d;		
		
	}
	
	public Double getId() {
		return id;
	}
	public void setId(Double id) {
		this.id = id;
	}
	public Double[] getAttribute() {
		return attribute;
	}
	public void setAttribute(Double[] attribute) {
		this.attribute = attribute;
	}
	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}
	public boolean equals(Object o){
		if(o instanceof Tuple){
			return this.id==((Tuple)o).id;
		}
		return false;
	}
	public int compareTo(Tuple o){
		return this.threshold.compareTo(o.threshold);
	}
	public Double computeThreshold(Double V[]){
		threshold = 0d;
		int n = V.length;
		for(int i=0;i<n;i++){
			threshold+=attribute[i]*V[i];
		}
		return threshold;
	}
	public Double getThreshold() {
		return threshold;
	}
	@Override
	public String toString() {
		String s = new String();
		s=s.concat(""+id);
		for(int i=0;i<attribute.length;i++){
			s=s.concat("	"+attribute[i]);
		}
		s=s.concat("	"+threshold);		
		return s;
	}
}


public class topk {
	List<List<IdAttribute>> outerList = new ArrayList<List<IdAttribute>>();
	List<BTree<Double, Double>> btreeList = new ArrayList<BTree<Double,Double>>();
	List<Tuple> table ;
	String header[];
	Double V[];
	int n,rows,k;
	public topk(int n, int k, Double V[]){
		this.n = n;
		this.k = k;
		this.V = V;
	}
	
	private void pqInsert(PriorityQueue<Tuple> pq, Tuple t){
		if(pq.size()<k){
			pq.offer(t);
		}
		else{
			if(t.getThreshold()>pq.peek().getThreshold()){
				pq.poll();
				//System.out.println(t.getThreshold());
				pq.offer(t);
			}
		}
	}
	
	private Tuple createTuple(Double id){
		Double attribute[] = new Double[n];
		int i=0;
		for(Iterator<BTree<Double, Double>> it = btreeList.iterator();it.hasNext();){
			attribute[i++] = it.next().get(id);
		}
		return new Tuple(id,attribute,V);
	}
	
	private Double computeThreshold(Double attribute[]){
		Double threshold = 0d;
		int n = V.length;
		for(int i=0;i<n;i++){
			threshold+=attribute[i]*V[i];
		}
		return threshold;
	}
	
	private boolean indexCreation(String inputFile){		
		BufferedReader inputBufferedReader = null;
		FileReader fr = null;
		String inputLine = "";
		String splitRegex = ",";	
		table = new ArrayList<Tuple>();
		
		try {
			fr = new FileReader(inputFile);
			inputBufferedReader = new BufferedReader(fr);
			String headerLine = inputBufferedReader.readLine();
			header= headerLine.split(","); 
			int headerSplitLength = header.length;
			for(int i=0;i<headerSplitLength-1;i++){
				List<IdAttribute> tempList = new ArrayList<IdAttribute>();
				outerList.add(tempList);
			}
			
			
			rows = 0;
			for(;(inputLine=inputBufferedReader.readLine())!=null;rows++) {
				String[] lineSplit = inputLine.split(splitRegex);
				Double attrValues[] = new Double[n];
				for(int i=0;i<outerList.size();i++){					
					attrValues[i] = Double.valueOf(lineSplit[i+1]);
					outerList.get(i).add(new IdAttribute(Double.valueOf(lineSplit[0]),Double.valueOf(lineSplit[i+1])));
				}				
				table.add(new Tuple(Double.valueOf(lineSplit[0]), attrValues));
				
			}
			
			for(List<IdAttribute> tempList:outerList){
				Collections.sort(tempList,new IdAttributeSort());
				BTree<Double, Double> tempBtree = new BTree<Double, Double>();
				for(IdAttribute idAttribute: tempList){
					tempBtree.put(idAttribute.id, idAttribute.attribute);
				}				
				btreeList.add(tempBtree);
			}
			
			
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException in reading input file");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			System.out.println("IOException in reading line from input file");
			e.printStackTrace();
			return false;
		} finally {
			if(inputBufferedReader != null){
				try {
					inputBufferedReader.close();
				} catch (IOException e) {
					System.out.println("IOException in closing inputBufferedReader");
					e.printStackTrace();
				}
			}
			try{
				fr.close();
			}
			catch(Exception e){
				
			}
		}		
		return true;
	}
	
	public void init(String fileName){
		if(!indexCreation(fileName)){
			System.exit(0);
		}
	}
	
	public Stack<Tuple> naiveAlgo(){
		PriorityQueue<Tuple> result = new PriorityQueue<Tuple>();
		
		Tuple cur = null;
		for(int i=0;i<table.size();i++){
			cur = table.get(i);
			//System.out.println(cur);
			cur.computeThreshold(V);
			//System.out.println(cur);
			pqInsert(result,cur);
		}
		Stack<Tuple> s = new Stack<Tuple>();
		while(!result.isEmpty()){			
			s.push(result.poll());
		}
		return s;
	}
	
	public Stack<Tuple> thresholdAlgo(){

		PriorityQueue<Tuple> result = new PriorityQueue<Tuple>();		
		IdAttribute curRecord = null;
		Double threshold = 0d;
		HashSet<Double> newKeys = new HashSet<Double>();
		Double attrValues[] = new Double[n];
		//Threshold Algorithm 
		for(int i=0;i<rows;i++){
			if(result.size()>=k && result.peek().getThreshold()>= threshold){		//check this
				break;
			}
			int j=0;
			for(Iterator<List<IdAttribute>> listIterator = outerList.iterator();listIterator.hasNext();j++){
				curRecord = listIterator.next().get(i);
				attrValues[j] = curRecord.getAttribute();				
				newKeys.add(curRecord.getId());
			}
			//System.out.println(i);
			threshold = computeThreshold(attrValues);
			for(Iterator<Double> newKeysIterator = newKeys.iterator();newKeysIterator.hasNext();){
				Double id = newKeysIterator.next();
				Tuple curTuple = createTuple(id);
				if(!result.contains(curTuple)){
					if(result.size()>=k){
						if(result.peek().getThreshold()<curTuple.getThreshold()){
							result.poll();
							result.offer(curTuple);
						}
					}
					else{
						result.offer(curTuple);
					}
				}
			}
		}	
		Stack<Tuple> s = new Stack<Tuple>();
		while(!result.isEmpty()){			
			s.push(result.poll());
		}
		return s;
	}
	
	public void printStack(Stack<Tuple> s){
		for(String st:header){
			System.out.print(st+"\t" );			
		}
		System.out.println("Threshold");
		while(!s.isEmpty()){
			System.out.println(s.pop()+"\t" );
		}
	}
		
	public void printTable(){
		for(Iterator<Tuple> it = table.iterator();it.hasNext();){
			System.out.println(it.next());
			
		}
	}
	
	/*==============================================Join Implementation=================*/
	
	

	private BTree<Double, Tuple> btreeContructionJoin(ArrayList<Tuple> tupleList, int columnNo){
		BTree<Double, Tuple> tempBtree = new BTree<>();
		
	try {
		for(Tuple tuple : tupleList){
			// 0 indicates that join is on 1st column of our table
			if(columnNo == 0){
				tempBtree.put(tuple.getId(),tuple);
			}else{
				tempBtree.put(tuple.getAttribute()[columnNo-1],tuple);
			}
		}
	} catch (ArrayIndexOutOfBoundsException e) {
		System.out.println("ArrayIndexOutOfBoundsException in btreeContructionJoin");
	}	
		return tempBtree;
	}
	private ArrayList<Tuple> getTable(ArrayList<String> headerList,String fileName){
		ArrayList<Tuple> table = new ArrayList<Tuple>();
		BufferedReader inputBufferedReader = null;
		FileReader fr = null;
		String inputLine = "";
		String splitRegex = ",";
		
		
		try {
			fr = new FileReader(fileName);
			inputBufferedReader = new BufferedReader(fr);
			String headers[] = inputBufferedReader.readLine().split(splitRegex);
			
			for(int i=0;i<headers.length;i++){
				headerList.add(headers[i]);
			}
			
			while((inputLine=inputBufferedReader.readLine())!=null) {
				String[] lineSplit = inputLine.split(splitRegex);
				Double attrValues[] = new Double[headers.length-1];
				for(int i=0;i<headers.length-1;i++){					
					attrValues[i] = Double.valueOf(lineSplit[i+1]);					
				}					
				table.add(new Tuple(Double.valueOf(lineSplit[0]), attrValues));
				
			}			
			
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException in reading input file");
			e.printStackTrace();
			
		} catch (IOException e) {
			System.out.println("IOException in reading line from input file");
			e.printStackTrace();
			
		} finally {
			if(inputBufferedReader != null){
				try {
					inputBufferedReader.close();
				} catch (IOException e) {
					System.out.println("IOException in closing inputBufferedReader");
					e.printStackTrace();
				}
			}
			try{
				fr.close();
			}
			catch(Exception e){
				
			}
		}
		return table;
	}
	private int getColumnNo(ArrayList<String> headerList, String columnName){
		return headerList.indexOf(columnName);
	}
	private ArrayList<Tuple> createJoinTableUtility(ArrayList<Tuple> table1,ArrayList<Tuple> table2,BTree<Double, Tuple> btree,int columnNo1,int columnNo2,ArrayList<String> headerList1,ArrayList<String> headerList2,ArrayList<String> finalheaderList){
		Double key = null;
		Tuple tuple2 = null;
		ArrayList<Tuple> finalTupleList = new ArrayList<>();
		int attributeCounter1 = headerList1.size(),attributeCounter2 = headerList2.size();
		int finalAttributeCounter = attributeCounter1 + attributeCounter2 -1;
		finalheaderList.add(0,headerList1.get(columnNo1));
		for(int i=0;i<attributeCounter1;i++){
			if(i==columnNo1)
				continue;
			finalheaderList.add(headerList1.get(i));
		}
		for(int i=0;i<attributeCounter2;i++){
			if(i==columnNo2)
				continue;
			finalheaderList.add(headerList2.get(i));
		}
		//yet to handle duplicate keys
		for(Tuple tuple1 : table1){
			if(columnNo1==0){
				key=tuple1.getId();
			}
			else{
				key = tuple1.getAttribute()[columnNo1-1];
			}
			for(tuple2 = btree.get(key);tuple2!=null;tuple2 = btree.get(key)){
				
			
			rows++;
			int i =0;
			Double[] tempAttributes = new Double[finalAttributeCounter];
			if(columnNo1==0){
				tempAttributes[i++]=tuple1.getId();
				for(int j=0;j<attributeCounter1-1;j++)
					tempAttributes[i++] = tuple1.getAttribute()[j];
			}
			else{
				tempAttributes[i++]=tuple1.getAttribute()[columnNo1-1];
				tempAttributes[i++]=tuple1.getId();
				for(int j=0;j<attributeCounter1-1;j++){
					if(j==columnNo1-1)
						continue;
					tempAttributes[i++] = tuple1.getAttribute()[j];
				}
			}
			if(columnNo2==0){
				//tempAttributes[i++]=tuple2.getId();
				for(int j=0;j<attributeCounter2-1;j++)
					tempAttributes[i++] = tuple2.getAttribute()[j];
			}
			else{
				//tempAttributes[i++]=tuple2.getAttribute()[columnNo2-1];
				tempAttributes[i++]=tuple2.getId();
				for(int j=0;j<attributeCounter2-1;j++){
					if(j==columnNo2-1)
						continue;
					tempAttributes[i++] = tuple2.getAttribute()[j];
				}
			}
			finalTupleList.add(new Tuple(key,tempAttributes));
			btree.getTwo(key);
			}			
		}
		return finalTupleList; 
	}
	private ArrayList<Tuple> createJoinTable(String[] fileNames, String []joinAttribute1, String []joinAttribute2){
		ArrayList<Tuple> table1 = new ArrayList<Tuple>();
		ArrayList<Tuple> table2 = new ArrayList<Tuple>();
		BTree<Double, Tuple> btree = null;
		ArrayList<String> headerList1 =new ArrayList<String>(); 
		table1 = getTable(headerList1,fileNames[0]);		
		for(int i=1;i<fileNames.length;i++){
			ArrayList<String> headerList2 =new ArrayList<String>();			
			table2 = getTable(headerList2,fileNames[i]);
			int columnNo1 = getColumnNo(headerList1,joinAttribute1[i-1]);
			int columnNo2 = getColumnNo(headerList2,joinAttribute2[i-1]);
			btree = btreeContructionJoin(table2, columnNo2);
			ArrayList<String> tempList =new ArrayList<String>();
			table1 = createJoinTableUtility(table1,table2,btree,columnNo1,columnNo2,headerList1,headerList2,tempList);
			headerList1 = tempList;
		}
		header = headerList1.toArray(new String[headerList1.size()]);
		
		return table1;
	}
	
	public static void main(String[] args) {			
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String input = null,fileName = null;			
			int k = Integer.parseInt(args[0]);
			int n = Integer.parseInt(args[1]);
			Double V[] = new Double[n];
			topk joinTopK = null;
			String[] finalFileNames = null;
			String[] joinConditionOne = null;
			String[] joinConditionTwo = null;
			boolean flag = true;
			
			
			try{
				input = br.readLine();
			}
			catch(IOException e){
				System.out.println("Error caught while taking input from user");
				System.exit(0);
			}
			String temp[] = input.split(" ");
			if(temp==null || !temp[0].equalsIgnoreCase("init")){				
				System.out.println("Invalid Input "+temp.length);
				System.exit(0);
			}
			if(temp.length!=2){
				  flag=false;
				  String[] commandLine = input.split(" ");
				  finalFileNames = commandLine[1].split(",");
				  String[] conditionArray = commandLine[3].split(",");
				  int numberOfJoinConditions = conditionArray.length;
				  joinConditionOne = new String[numberOfJoinConditions];
				  joinConditionTwo = new String[numberOfJoinConditions];				  
				  
				  for(int i=0;i<numberOfJoinConditions;i++){
					  String[] tempArray = conditionArray[i].split("=");
					  String temp1[] = tempArray[0].split("\\.");
					  String temp2[] = tempArray[1].split("\\.");
					  joinConditionOne[i] = temp1[1];
					  joinConditionTwo[i] = temp2[1];					  
				  }
			}
			else{
				fileName = temp[1];
			}
			try{
				input = br.readLine();
			}
			catch(IOException e){
				System.out.println("Error caught while taking input from user");
				System.exit(0);
			}
			temp = input.split(" ");
			for(int i=1;i<=n;i++){
				V[i-1]=Double.parseDouble(temp[i]);
			}			
			joinTopK = new topk(n, k, V);
			if(!flag){
				joinTopK.table=joinTopK.createJoinTable(finalFileNames, joinConditionOne, joinConditionTwo);
			}
			else{
				joinTopK.init(fileName);
			}
			if(temp[0].equalsIgnoreCase("run1")){
				joinTopK.printStack(joinTopK.thresholdAlgo());
			}
			else if(temp[0].equalsIgnoreCase("run2")){				
				joinTopK.printStack(joinTopK.naiveAlgo());
			}
			else{
				System.out.println("Invalid Input "+temp.length);
				System.exit(0);
			}
		}
}

