package com.example.attendancetracker;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.attendancetracker.Lectures.Lectures;
import com.example.attendancetracker.databinding.FragmentFirstBinding;
import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class FirstFragment extends Fragment {

    final int ABSENT = 0;
    final int ATTENDED = 1;
    final int CANCELLED = 2; // Values attached to saving data
    private FragmentFirstBinding binding;
    private String previouslySelectedDate; //Used throughout to write to csv files
    Calendar calendar;
    String[] mornLectures, noonLectures, eveningLectures;

    List<Integer> currentState = new ArrayList<>(); // Holds all the values for the current page open
    List<Integer> fileState = new ArrayList<>(); // Holds values from the csv file read. null if the file doesnt exist
    List<Button> buttons = new ArrayList<>(); // Button list to be displayed on the screen
    List<String> allLectures = new ArrayList<>(); //List that has all the lectures of that day

    int i = 0;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        try {
            //getting current date and setting it
            calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) ;
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            previouslySelectedDate = String.format("%02d%02d%04d", dayOfMonth, month+1, year);

            //Creating file if it doesnt exist
            createFile(previouslySelectedDate);
            checkForGarbageData(year, month, dayOfMonth);

            return binding.getRoot();

        } catch (Exception e) {
            Log.e("DEBUG", "on create", e);
        }
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        deleteCSV();
//        readLogs(); //Debug flags

        //Reading CSV files and populating the file state
        createFile(previouslySelectedDate);


        //Defining tables for Morning, noon and after lunch sessions.
        //Morning
        TableLayout TMorn = binding.TMorn;
        TableRow TRMorn = new TableRow(requireContext());
        TRMorn.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));

        //Noon
        TableLayout TNoon = binding.TNoon;
        TableRow TRNoon = new TableRow(requireContext());
        TRNoon.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));

        //Afternoon
        TableLayout TAfternoon = binding.TAfterNoon;
        TableRow TRAfternoon = new TableRow(requireContext());
        TRAfternoon.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT));

        //Implementing a calendar view for ease of use
        Button calendarButton = binding.calendar;
        CalendarView calendarView = binding.calendarView;
        calendarView.setVisibility(View.GONE);

        setButtons(
                TRMorn, TRNoon, TRAfternoon,
                TMorn, TNoon, TAfternoon
        );

        //Toggling visibility of the calendar
        calendarButton.setOnClickListener(v -> {

            if (calendarView.getVisibility() == View.VISIBLE) {
                calendarView.setVisibility(View.GONE);
            } else {
                calendarView.setVisibility(View.VISIBLE);
            }
        });

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {

            saveData(currentState, fileState);

            fileState.clear();
            currentState.clear();

            previouslySelectedDate = String.format("%02d%02d%04d", dayOfMonth, month + 1, year);

            createFile(previouslySelectedDate);
            checkForGarbageData(year, month, dayOfMonth);

            setButtons(
                    TRMorn, TRNoon, TRAfternoon,
                    TMorn, TNoon, TAfternoon
            );
        });
    }

    private void createFile(String name) {

        File current = new File(getActivity().getFilesDir(), name + ".csv");

        if (!current.exists()) {
            try {
                current.createNewFile();
            } catch (Exception e) {
                Log.e("DEBUG", "", e);
            }

        }
    }

    private void setButtons(
            TableRow TRMorn, TableRow TRNoon, TableRow TRAfternoon,
            TableLayout TMorn, TableLayout TNoon, TableLayout TAfternoon
    ) {
        if (TMorn.getParent() != null) {
            try {
                TRMorn.removeAllViews();
                TRNoon.removeAllViews();
                TRAfternoon.removeAllViews();
                TMorn.removeAllViews();
                TNoon.removeAllViews();
                TAfternoon.removeAllViews();
            } catch (Exception e) {
                Log.e("DEBUG", "Actual error: ", e);
            }
        }

        if (!fileState.isEmpty()) {
            currentState.clear();
            currentState.addAll(fileState);
        }

        if (currentState.isEmpty())
            for (int i = 0; i < (mornLectures.length + noonLectures.length + eveningLectures.length); i++) currentState.add(0);

        buttons.clear();

        i = 0;
        setListeners(mornLectures, TRMorn, allLectures, currentState, buttons);
        setListeners(noonLectures, TRNoon, allLectures, currentState, buttons);
        setListeners(eveningLectures, TRAfternoon, allLectures, currentState, buttons);

        TMorn.addView(TRMorn);
        TNoon.addView(TRNoon);
        TAfternoon.addView(TRAfternoon);
    }

    private void setListeners(
            String[] lectures,
            TableRow tableRow,
            List<String> allLectures,
            List<Integer> currentState,
            List<Button> buttons
    ) {
        for (String a : lectures) {
            Button temp = new Button(requireContext());
            temp.setText(a);
            temp.setTag(a);

            //if file state has loaded up, then set the corresponding colors
            if (!fileState.isEmpty()) {
                String bgColor;
                switch (fileState.get(i)) {
                    case ABSENT: {
                        bgColor = "#FF0000";
                    }
                    break;
                    case ATTENDED: {
                        bgColor = "#8cff1a";
                    }
                    break;
                    case CANCELLED: {
                        bgColor = "#3d3d5c";
                    }
                    break;
                    default:
                        bgColor = "#FFFFFF";
                }
                i++;
                temp.setBackgroundColor(Color.parseColor(bgColor));
            }

            temp.setOnClickListener(v -> {

                int tempIndex = allLectures.indexOf(temp.getTag());
                String bgColor;

                if (currentState.get(tempIndex) != CANCELLED) {
                    currentState.set(tempIndex, currentState.get(tempIndex) ^ 1);
                } else {
                    currentState.set(tempIndex, 0);
                }

                switch (currentState.get(tempIndex)) {
                    case 0: {
                        bgColor = "#FF0000";
                    }
                    break;
                    case 1: {
                        bgColor = "#8cff1a";
                    }
                    break;
                    default:
                        bgColor = "#FFFFFF";
                }

                buttons.get(tempIndex).setBackgroundColor(Color.parseColor(bgColor));
            });
            temp.setOnLongClickListener(v -> {
                currentState.set(allLectures.indexOf(temp.getTag()), CANCELLED);
                buttons.get(allLectures.indexOf(temp.getTag())).setBackgroundColor(Color.parseColor("#3d3d5c"));
                return true;
            });
            buttons.add(temp);
            tableRow.addView(temp);
        }
    }

    private void setLectures(
            List<String> allLectures, int dayOfWeek
    ) {
        switch (dayOfWeek + 1) {
            case 2: {
                mornLectures = Lectures.MONmornLectures;
                noonLectures = Lectures.MONnoonLectures;
                eveningLectures = Lectures.MONeveningLectures;
            }
            break;
            case 3: {
                mornLectures = Lectures.TUEmornLectures;
                noonLectures = Lectures.TUEnoonLectures;
                eveningLectures = Lectures.TUEeveningLectures;
            }
            break;
            case 4: {
                mornLectures = Lectures.WEDmornLectures;
                noonLectures = Lectures.WEDnoonLectures;
                eveningLectures = Lectures.WEDeveningLectures;
            }
            break;
            case 5: {
                mornLectures = Lectures.THUmornLectures;
                noonLectures = Lectures.THUnoonLectures;
                eveningLectures = Lectures.THUeveningLectures;
            }
            break;
            case 6: {
                mornLectures = Lectures.FRImornLectures;
                noonLectures = Lectures.FRInoonLectures;
                eveningLectures = Lectures.FRIeveningLectures;
            }
            break;
            default: {
                mornLectures = Lectures.mornLectures;
                noonLectures = Lectures.noonLectures;
                eveningLectures = Lectures.eveningLectures;
            }
            break;
        }//Getting and setting the lectures of the corresponding day

        allLectures.clear();

        allLectures.addAll(Arrays.asList(mornLectures));
        allLectures.addAll(Arrays.asList(noonLectures));
        allLectures.addAll(Arrays.asList(eveningLectures));
    }

    private String stripQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        } else {
            return str;
        }
    }

    private void deleteCSV() {
        String[] fileList = getActivity().fileList();

        // Loop through each file in the fileList
        for (String fileName : fileList) {
            // Check if the file has a .csv extension
            if (fileName.endsWith(".csv")) {
                // Delete the file
                if (getActivity().deleteFile(fileName)) {
                    Log.d("DEBUG", "Deleted file: " + fileName);
                } else {
                    Log.d("DEBUG", "Failed to delete file: " + fileName);
                }
            }
        }
    }

    private void readLogs() {
        // List all files in the directory
        File[] files = getActivity().getFilesDir().listFiles();

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                Log.d("DEBUG", "Contents of " + file.getName() + ":");
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Log.d("DEBUG", line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file: " + file.getName());
                    e.printStackTrace();
                }
                System.out.println();
            }
        }
    }

    private void checkForGarbageData(int year, int month, int dayOfMonth) {
        File readFile = new File(getActivity().getFilesDir(), previouslySelectedDate + ".csv");

        LocalDate date = LocalDate.of(year, month + 1, dayOfMonth);
        DayOfWeek dayOf = date.getDayOfWeek();
        setLectures(allLectures, dayOf.getValue());

        try (BufferedReader reader = new BufferedReader(new FileReader(readFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                for (String a : fields) fileState.add(Integer.valueOf(stripQuotes(a))); // <- Doing the entire block for this line here
                if (fields.length < (mornLectures.length + noonLectures.length + eveningLectures.length)) {
                    try (FileWriter writer = new FileWriter(readFile, false)) {
                        writer.write("");
                    } catch (IOException e) {
                        Log.e("DEBUG", "", e);
                    }
                    fileState.clear();
                }
            }
        } catch (IOException e) {
            Log.e("DEBUG", "", e);
        }
    }

    private void readCSV(String name) {
        File readFile = new File(getActivity().getFilesDir(), name + ".csv");

        try (BufferedReader reader = new BufferedReader(new FileReader(readFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d("DEBUG", line);
            }
        } catch (IOException e) {
            Log.e("DEBUG", "Exception", e);
        }
    }

    private void saveData(List<Integer> localCurrentState, List<Integer> localFileState){
        String lastDate = previouslySelectedDate;
        //Writing the data of the previous date to the storage
        if (!localFileState.equals(localCurrentState)) {
            try {
                File file = new File(getActivity().getFilesDir(), lastDate + ".csv");
                FileWriter fileWriter = new FileWriter(file);

                CSVWriter csvWriter = new CSVWriter(fileWriter);

                List<String> tempList = new ArrayList<>();
                for (int a : localCurrentState)
                    tempList.add(String.valueOf(a));
                csvWriter.writeNext(tempList.toArray(new String[0]));
                csvWriter.close();
            } catch (IOException e) {
                Log.e("DEBUG", "Writing exception caught: ", e);
            }
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void onStop() {
        super.onStop();
        saveData(currentState, fileState);
    }

}