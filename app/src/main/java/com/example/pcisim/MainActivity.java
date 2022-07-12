package com.example.pcisim;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.*;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;
public class MainActivity extends AppCompatActivity {
    GraphView graph = null;
    int latencyTimer = 0;// время ожидания конца транзакции(снятия Devsel) -> timeout
    BigInteger BusNum = new BigInteger(String.valueOf(0),10); // 8 бит AD[23:16]
    BigInteger DeviceNum = new BigInteger(String.valueOf(1),10); // 5 бит AD[15:11]
    BigInteger FuncNum = new BigInteger(String.valueOf(1),10); // 3 бита AD[10:8]
    BigInteger BusNumMax = new BigInteger(String.valueOf(256),10);;
    BigInteger DeviceNumMax = new BigInteger(String.valueOf(32),10);;
    BigInteger FuncNumMax = new BigInteger(String.valueOf(7),10);;
    BigInteger RegNum = new BigInteger(String.valueOf(0),10); // 6 бит AD[7-2]
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        graph = findViewById(R.id.view);
        graph.setTitle("PCI Временная диаграмма");
    }
    private String invertString(String str)
    {
        String res = "";
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '0') res += 1;
            else if(str.charAt(i) == '1') res += 0;
            else res += str.charAt(i);
        }
        return res;
    }
    private ArrayList<Integer> stringToSequence(String str)
    {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '0'){ arr.add(0); }
            else arr.add(1);
        }
        return arr;
    }
    private LineGraphSeries<DataPoint> sequenceToSeries(ArrayList<Integer> data)
    {
        //func(seq -> series)
        // input: ArrayList<Integer> data
        // output: LineGraphSeries<DataPoint> series1
        ArrayList<DataPoint> graphPoints = new ArrayList<DataPoint>();
        double yVar = data.get(0);
        double xVar = 0.0;
        graphPoints.add(new DataPoint(xVar,yVar));
        xVar = xVar + 0.05;
        for(int i = 0; i < data.size(); i++)
        {
            yVar = data.get(i);
            graphPoints.add(new DataPoint(xVar,yVar));
            xVar = xVar + 0.9;
            graphPoints.add(new DataPoint(xVar,yVar));
            xVar = xVar + 0.1;
        }
        //ArrayList -> DataPoint[]
        DataPoint[] xx = new DataPoint[graphPoints.size()];
        for(int i = 0; i < graphPoints.size(); i++)
        {
            xx[i] = graphPoints.get(i);
        }
        LineGraphSeries<DataPoint> series1 = new LineGraphSeries<DataPoint>(xx);
        series1.setTitle("data");
        series1.setColor(Color.GREEN);
        return series1;
    }
    private String resizeStrTo(String str, int lengthStr, Boolean isInsertInBegin)
    {
        if(str.length()<lengthStr) {
            int numOfCharsToAdd = lengthStr-str.length();
            String newBinDID = "";
            while(numOfCharsToAdd>0){if(isInsertInBegin)newBinDID+='0'; else str += '0'; numOfCharsToAdd--;}
            str = newBinDID + str; }
        return str;
    }
    public static String reverseString(String str) {
        return new StringBuilder(str).reverse().toString();
    }
    private void lineSeriesToGraph(LineGraphSeries<DataPoint> series1, String title, int color, int thickness)
    {
        series1.setTitle(title);
        series1.setColor(color);
        series1.setThickness(thickness);
        graph.addSeries(series1);
    }
    private ArrayList<Integer> moveSeqUpDown(ArrayList<Integer> src, int delta)
    {
        for(int i = 0; i < src.size(); i++)
            src.set(i, src.get(i)+delta);
        return src;
    }
    public void newTransmission(View view) {
        graph.removeAllSeries();
        final Random random = new Random();
        //дешифровка адреса
        EditText etAddress = findViewById(R.id.editTextAddress);
        String address = etAddress.getText().toString();
        String buffer = "";
        int whichNumberNow = 0;
        for(int i = 0; i < address.length(); i++)
        {

            if(address.charAt(i) == ':')
            {
                //convert str to int (buffer)
                BigInteger bufferInt = new BigInteger(buffer, 10);
                if(whichNumberNow == 0) BusNum = bufferInt;
                else if (whichNumberNow == 1) DeviceNum = bufferInt;
                whichNumberNow++;
                buffer = "";
            }
            else
                buffer += address.charAt(i);
            if(i == address.length()-1)
            {
                BigInteger bufferInt = new BigInteger(buffer, 10);
                FuncNum = bufferInt;
            }
        }
        if((BusNum.compareTo(BusNumMax) == -1)&&(DeviceNum.compareTo(DeviceNumMax) == -1)&&(FuncNum.compareTo(FuncNumMax) == -1))
        //создание строки адреса. передача данных
        {
            address = "00000000:";
            address += resizeStrTo(BusNum.toString(2),8,true) + ":" + resizeStrTo(DeviceNum.toString(2),5,true) + ":";
            address += resizeStrTo(FuncNum.toString(2), 3,true) + ":" + resizeStrTo(RegNum.toString(2), 6,true);
            TextView tvAdress = findViewById(R.id.textView3);
            tvAdress.setText("AD: фаза адреса - адрес = " + address);
        }
        String DataSendingNow = "";
        EditText etData = findViewById(R.id.editTextData);
        TextView tvData = findViewById(R.id.textView4);
        String data = etData.getText().toString();
        //отрисовка+алгоритм
        String Req = "0";
        String Gnt = "0";
        String Frame = "0";
        String Irdy = "0";
        String Trdy = "0";
        String DevSel = "0";
        String Rst = "0";
        String AD = "0";
        String CBE = "0";
        Boolean ReqSet = false;
        Boolean timeout = false;
        Boolean frameDrop = false;
        CheckBox cbTimeout = findViewById(R.id.checkBox);
        if(cbTimeout.isChecked())
        {
            timeout = true;
            EditText etTimer = findViewById(R.id.editTextTimer);
            BigInteger bufferInt = new BigInteger(etTimer.getText().toString(), 10);
            latencyTimer = bufferInt.intValue();
        }
        int bitesTransmitted = 0;
        ArrayList<Integer> arr = new ArrayList<Integer>(); for(int i = 0; i < 9; i++) { arr.add(0); }
        int i = 0;//i - такты
        while(true)
        {
            if((i>latencyTimer)&&(timeout==true))
                break;
            if(ReqSet)
            {
                if(Gnt.charAt(i)=='1')
                {
                    if(bitesTransmitted>= data.length())
                        break;
                    if(i==2)
                    {
                        Frame += '1'; arr.set(2,1);
                        AD += '1'; arr.set(7,1);
                        CBE += "1"; arr.set(8,1);
                    }
                    else if(DevSel.charAt(i) == '0')
                    {
                        DevSel += '1'; arr.set(5,1);
                    }
                    else if ((data.length() - bitesTransmitted <= 32)&&(!frameDrop))
                    {
                        //если это предпоследняя передача
                        Frame += '0'; arr.set(2,1);
                        frameDrop = true;
                    }
                    else
                    {
                        if((Trdy.charAt(i)=='1')&&(Irdy.charAt(i) == '1'))
                        {
                            //рядовая фаза данных
                            AD += '1'; arr.set(7,1); arr.set(7,1);
                            CBE += '1'; arr.set(8,1); arr.set(8,1);
                            bitesTransmitted += 32;
                        }
                        //ожидание готовности устройств
                        if(random.nextInt(10)>7)
                            Irdy += '1';
                        else
                            Irdy += '0';
                        if(random.nextInt(10)>7)
                            Trdy += '1';
                        else
                            Trdy += '0';
                        arr.set(3,1);
                        arr.set(4,1);
                    }
                }
                else { Gnt += '1'; arr.set(1,1); }
            }
            else { Req += '1'; arr.set(0,1); ReqSet = true;}
            for(int j = 0; j < arr.size(); j++)
            {
                if(arr.get(j) == 0)
                {
                    if(j == 0) Req += '0';
                    else if (j == 7) AD += '0';
                    else if (j == 8) CBE += '0';
                    else
                    {
                        if(j == 1) Gnt += Gnt.charAt(Gnt.length()-1);
                        else if (j == 2) Frame += Frame.charAt(Frame.length()-1);
                        else if(j == 3) Irdy += Irdy.charAt(Irdy.length()-1);
                        else if (j == 4) Trdy += Trdy.charAt(Trdy.length()-1);
                        else if (j == 5) DevSel += DevSel.charAt(DevSel.length()-1);
                        else if (j == 6) Rst += Rst.charAt(Rst.length()-1);
                    }
                }
                arr.set(j,0);
            }
            i++;
        }
        Req += '0';Req += '0';
        Gnt += '0';Gnt += '0';
        Frame += '0';Frame += '0';
        Irdy += '0';Irdy += '0';
        Trdy += '0';Trdy += '0';
        DevSel += '0';DevSel += '0';
        Rst += '1';Rst += '1';
        AD += '0'; AD += '0';
        CBE += '0'; CBE += '0';

        if(bitesTransmitted < data.length())//если тайм-аут
        {
            tvData.setText("Ошибка передачи данных - тайм-аут");
        }
        else {
            tvData.setText("Размер введенных данных: " + data.length() + "\n AD: фаза данных - данные:");
            int k = (int) Math.ceil(data.length() / 32.);
            for(int j = 0; j < k; j++)
            {
                String tmp = "";
                if(j == k-1)
                {
                    //если это последний пакет. мб неполным. дополняем.
                    String lastData = resizeStrTo(reverseString(data.substring(j*32)), 32, true);
                    tmp = tvData.getText().toString();
                    DataSendingNow = lastData;
                }
                else {

                    tmp = tvData.getText().toString();
                    String adds = data.substring(j*32, (j+1)*32);
                    DataSendingNow = reverseString(adds);
                }
                tvData.setText(tmp + "\n[" + (j+1) + "] " + DataSendingNow);
            }
        }
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertString(Req)),16)),"REQ#",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertString(Gnt)),14)),"Gnt#",Color.BLUE, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertString(Frame)),12)),"Frame#",Color.RED, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertString(Irdy)),10)),"Irdy#",Color.DKGRAY, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertString(Trdy)),8)),"Trdy#",Color.GRAY, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertString(DevSel)),6)),"DevSel#",Color.GREEN, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertString(Rst)),4)),"Rst#",Color.MAGENTA, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertString(AD)),2)),"AD#",Color.LTGRAY, 3);
        lineSeriesToGraph(sequenceToSeries(stringToSequence(invertString(CBE))),"CBE#",Color.CYAN, 3);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(18);
        graph.getLegendRenderer().setVisible(true);
    }
}