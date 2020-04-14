/*
 * NextBill Android client application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.client.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.PieData;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.database.MainDatabaseHandler;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BasicDataSubType;
import de.nextbill.client.enums.DiagrammCategoryEnum;
import de.nextbill.client.enums.DiagrammTypeEnum;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.helper.ChartHelper;
import de.nextbill.client.helper.SearchHelper;
import de.nextbill.client.interfaces.XAxisGeneralFormatter;
import de.nextbill.client.model.BasicData;
import de.nextbill.client.pojos.DiagrammDataChart;
import de.nextbill.client.pojos.DiagrammTimeSpinner;
import de.nextbill.client.pojos.SearchFilter;
import de.nextbill.client.utils.SqlBuilder;


public class ActivityAnalysisTabTimeRange extends BaseActivity {

    static int INVOICE_DETAILS = 2;

    private BarChart chart1;
    private PieChart chart2;
    private Spinner spinner;
    private LinearLayout lineDiagrammLinearLayout;
    private LinearLayout cakeDiagrammLinearLayout;
    private ImageView leftArrowIv;
    private ImageView rightArrowIv;
    private Spinner dateSelectionSpinner;
    private LinearLayout dateControlLinearLayout;
    private TextView filterTv;
    private RelativeLayout progressBarRl;
    private RelativeLayout analysisControlRl;

    private List<DiagrammCategoryEnum> diagrammCategoryEnums = new ArrayList<>();
    private DiagrammCategoryEnum diagrammCategoryEnum = DiagrammCategoryEnum.TIME_RANGE;
    private DiagrammTypeEnum diagrammTypeEnum = DiagrammTypeEnum.LINE_DIAGRAM;
    private List<DiagrammTimeSpinner> diagrammTimeSpinnerList = new ArrayList<>();
    private DiagrammTimeSpinner diagrammTimeSpinner;

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        getLayoutInflater().inflate(R.layout.activity_analysis_fragment_time_range, frameLayout);
        this.setTitle("Auswertungen");

        filterTv = (TextView) findViewById(R.id.filterTv);
        chart1 = (BarChart) findViewById(R.id.chart1);
        chart2 = (PieChart) findViewById(R.id.chart2);
        lineDiagrammLinearLayout = (LinearLayout) findViewById(R.id.lineDiagrammLinearLayout);
        cakeDiagrammLinearLayout = (LinearLayout) findViewById(R.id.cakeDiagrammLinearLayout);
        chart2 = (PieChart) findViewById(R.id.chart2);
        spinner = (Spinner) findViewById(R.id.spinner);
        dateSelectionSpinner = (Spinner) findViewById(R.id.dateSelectionSpinner);
        leftArrowIv = (ImageView) findViewById(R.id.leftArrowIv);
        rightArrowIv = (ImageView) findViewById(R.id.rightArrowIv);
        dateControlLinearLayout = (LinearLayout) findViewById(R.id.dateControlLinearLayout);
        progressBarRl = (RelativeLayout) findViewById(R.id.progressBarRl);
        analysisControlRl = (RelativeLayout) findViewById(R.id.analysisControlRl);

        diagrammCategoryEnums.clear();
        diagrammCategoryEnums.addAll(Arrays.asList(DiagrammCategoryEnum.values()));
        ArrayAdapter<DiagrammCategoryEnum> dataAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, diagrammCategoryEnums);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                diagrammCategoryEnum = diagrammCategoryEnums.get(position);

                refreshViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        rightArrowIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer currentIndex = diagrammTimeSpinnerList.indexOf(diagrammTimeSpinner);
                currentIndex = currentIndex -1;
                diagrammTimeSpinner = diagrammTimeSpinnerList.get(currentIndex);
                BasicData basicDataStartDateNew = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_START);
                BasicData basicDataEndDateNew = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_END);
                basicDataStartDateNew.setNumberValue(new BigDecimal(diagrammTimeSpinner.getStartDate().getTime()));
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicDataStartDateNew);
                basicDataEndDateNew.setNumberValue(new BigDecimal(diagrammTimeSpinner.getEndDate().getTime()));
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicDataEndDateNew);

                if (currentIndex == (diagrammTimeSpinnerList.size()-1)){
                    rightArrowIv.setVisibility(View.INVISIBLE);
                }else{
                    rightArrowIv.setVisibility(View.VISIBLE);
                }
                if (dateSelectionSpinner.getSelectedItemPosition() != currentIndex){
                    dateSelectionSpinner.setSelection(currentIndex);
                }
            }
        });

        leftArrowIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer currentIndex = diagrammTimeSpinnerList.indexOf(diagrammTimeSpinner);
                currentIndex = currentIndex +1;
                diagrammTimeSpinner = diagrammTimeSpinnerList.get(currentIndex);
                BasicData basicDataStartDateNew = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_START);
                BasicData basicDataEndDateNew = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_END);
                basicDataStartDateNew.setNumberValue(new BigDecimal(diagrammTimeSpinner.getStartDate().getTime()));
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicDataStartDateNew);
                basicDataEndDateNew.setNumberValue(new BigDecimal(diagrammTimeSpinner.getEndDate().getTime()));
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicDataEndDateNew);

                if (currentIndex == 0){
                    leftArrowIv.setVisibility(View.INVISIBLE);
                }else{
                    leftArrowIv.setVisibility(View.VISIBLE);
                }
                if (dateSelectionSpinner.getSelectedItemPosition() != currentIndex){
                    dateSelectionSpinner.setSelection(currentIndex);
                }
            }
        });

        diagrammTimeSpinnerList = generateTimeSpinnerBeans();
        diagrammTimeSpinner = diagrammTimeSpinnerList.get(0);
        ArrayAdapter<DiagrammTimeSpinner> dataAdapter2 = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_item, diagrammTimeSpinnerList);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSelectionSpinner.setAdapter(dataAdapter2);

        dateSelectionSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                diagrammTimeSpinner = diagrammTimeSpinnerList.get(position);
                refreshViews();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        lineDiagrammLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diagrammTypeEnum = DiagrammTypeEnum.LINE_DIAGRAM;
                refreshViews();
            }
        });

        cakeDiagrammLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diagrammTypeEnum = DiagrammTypeEnum.CAKE_DIAGRAM;
                refreshViews();
            }
        });

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_menu_white_24);
        getSupportActionBar().setHomeAsUpIndicator(drawable);

        refreshBasicViews();
    }

    public List<DiagrammTimeSpinner> generateTimeSpinnerBeans(){
        List<DiagrammTimeSpinner> diagrammTimeSpinnerList = new ArrayList<>();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy, MMMM");
        for (int i = 0; i < 6; i++){
            DiagrammTimeSpinner diagrammTimeSpinner = new DiagrammTimeSpinner();

            Calendar month = Calendar.getInstance();
            month.add(Calendar.MONTH, -1 * i);
            Integer firstDayOfMonth = month.getActualMinimum(Calendar.DAY_OF_MONTH);
            Integer lastDayOfMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar firstDay = (Calendar) month.clone();
            Calendar lastDay = (Calendar) month.clone();
            firstDay.set(Calendar.DAY_OF_MONTH, firstDayOfMonth);
            firstDay.set(Calendar.HOUR, 0);
            firstDay.set(Calendar.MINUTE, 0);
            firstDay.set(Calendar.SECOND, 0);
            firstDay.set(Calendar.MILLISECOND, 0);
            lastDay.set(Calendar.DAY_OF_MONTH, lastDayOfMonth);
            lastDay.set(Calendar.HOUR, 0);
            lastDay.set(Calendar.MINUTE, 0);
            lastDay.set(Calendar.SECOND, 0);
            lastDay.set(Calendar.MILLISECOND, 0);
            Date firstDayDate = firstDay.getTime();
            Date lastDayDate = lastDay.getTime();

            diagrammTimeSpinner.setStartDate(firstDayDate);
            diagrammTimeSpinner.setEndDate(lastDayDate);
            diagrammTimeSpinner.setDisplayName(simpleDateFormat.format(firstDayDate));

            diagrammTimeSpinnerList.add(diagrammTimeSpinner);
        }

        return diagrammTimeSpinnerList;
    }

    public void setFilterText(boolean show, String text){
        if (show && filterTv != null){
            filterTv.setText("Filter: " + text);
            filterTv.setVisibility(View.VISIBLE);
        }else{
            filterTv.setVisibility(View.GONE);
        }
    }

    public void refreshDiagram(){
        AsyncTask refreshInvoiceListTask = new RefreshDiagramTask().execute();
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.analysis, menu);
        return true;
    }

    public void startFilterActivity(){
        Intent intent = new Intent(ActivityAnalysisTabTimeRange.this, ActivitySearchConfiguration.class);
        startActivityForResult(intent, ACTIVITY_SEARCH);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.filterMenuButton:
                startFilterActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == ACTIVITY_SEARCH && resultCode == RESULT_OK) {
            refreshViews();
        }
    }

    public void refreshBasicViews(){

        BasicData basicDataStartDateNew = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_START);
        BasicData basicDataEndDateNew = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.DATE_END);

        boolean isCustomizedFilter = false;
        if (basicDataStartDateNew != null && basicDataEndDateNew != null){

            Date startDate = new Date();
            startDate.setTime(basicDataStartDateNew.getNumberValue().longValue());
            Calendar month = Calendar.getInstance();
            Calendar firstDay = (Calendar) month.clone();
            firstDay.setTime(startDate);
            firstDay.set(Calendar.HOUR, 0);
            firstDay.set(Calendar.MINUTE, 0);
            firstDay.set(Calendar.SECOND, 0);
            firstDay.set(Calendar.MILLISECOND, 0);
            Date firstDayDate = firstDay.getTime();

            Date endDate = new Date();
            endDate.setTime(basicDataEndDateNew.getNumberValue().longValue());
            Calendar lastDay = (Calendar) month.clone();
            lastDay.setTime(endDate);
            lastDay.set(Calendar.HOUR, 0);
            lastDay.set(Calendar.MINUTE, 0);
            lastDay.set(Calendar.SECOND, 0);
            lastDay.set(Calendar.MILLISECOND, 0);
            Date lastDayDate = lastDay.getTime();

            if (!firstDayDate.equals(diagrammTimeSpinner.getStartDate()) || !lastDayDate.equals(diagrammTimeSpinner.getEndDate())){
                isCustomizedFilter = true;
            }
        }

        if (isCustomizedFilter){
            dateControlLinearLayout.setVisibility(View.GONE);
        }else{

            basicDataStartDateNew = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.DATE_START);
            basicDataEndDateNew = MainDatabaseHandler.findBasicSearchDataOrCreate(getApplicationContext(), BasicDataSubType.DATE_END);

            if (diagrammCategoryEnum.equals(DiagrammCategoryEnum.TIME_RANGE)) {
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicDataStartDateNew);
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicDataEndDateNew);
                dateControlLinearLayout.setVisibility(View.GONE);
            }else if (diagrammCategoryEnum.equals(DiagrammCategoryEnum.TIME_RANGE_DAYS)){
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicDataStartDateNew);
                MainDatabaseHandler.deleteBasicData(getApplicationContext(), basicDataEndDateNew);
                dateControlLinearLayout.setVisibility(View.GONE);
            }else{
                basicDataStartDateNew.setNumberValue(new BigDecimal(diagrammTimeSpinner.getStartDate().getTime()));
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicDataStartDateNew);
                basicDataEndDateNew.setNumberValue(new BigDecimal(diagrammTimeSpinner.getEndDate().getTime()));
                MainDatabaseHandler.saveBasicSearchData(getApplicationContext(), basicDataEndDateNew);
                dateControlLinearLayout.setVisibility(View.VISIBLE);
            }

            Integer currentIndex = diagrammTimeSpinnerList.indexOf(diagrammTimeSpinner);
            if (currentIndex == (diagrammTimeSpinnerList.size()-1)){
                leftArrowIv.setVisibility(View.INVISIBLE);
            }else{
                leftArrowIv.setVisibility(View.VISIBLE);
            }
            if (currentIndex == 0){
                rightArrowIv.setVisibility(View.INVISIBLE);
            }else{
                rightArrowIv.setVisibility(View.VISIBLE);
            }
        }

        BasicData basicDataExpense = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.EXPENSE);
        BasicData basicDataIncome = MainDatabaseHandler.findBasicDataSearchValue(getApplicationContext(), BasicDataSubType.INCOME);
        if ((basicDataExpense != null && basicDataIncome != null) || (basicDataExpense == null && basicDataIncome == null)){
            diagrammTypeEnum = DiagrammTypeEnum.LINE_DIAGRAM;
            cakeDiagrammLinearLayout.setVisibility(View.INVISIBLE);
        }else{
            cakeDiagrammLinearLayout.setVisibility(View.VISIBLE);
        }

    }

    public void refreshViews(){

        StatusDatabaseHandler statusDatabaseHandler = StatusDatabaseHandler.getInstance(getApplicationContext());
        int status = statusDatabaseHandler.getStatus();

        analysisControlRl.setVisibility(View.INVISIBLE);
        progressBarRl.setVisibility(View.VISIBLE);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                refreshBasicViews();

                refreshDiagram();
            }
        }, 100);

    }

    public class RefreshDiagramTask extends AsyncTask<Object, Integer, List<Object>> {
        @Override
        protected List<Object> doInBackground(Object... objects) {

            SqlBuilder sqlBuilder = new SqlBuilder(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.TABLE_INVOICE);
            sqlBuilder.addTable(MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM, MainDatabaseHandler.TABLE_COST_DISTRIBUTION_ITEM);
            sqlBuilder
                    .startBracket()
                    .isNotEqualFields(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_ID, MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
                    .or()
                    .isNull(MainDatabaseHandler.TABLE_INVOICE, MainDatabaseHandler.VAR_INVOICE_STANDING_ORDER_INVOICE_TEMPLATE_ID)
                    .endBracket()
                    .and()
                    .isEqual(MainDatabaseHandler.VAR_INVOICE_STATUS_ENUM, InvoiceStatusEnum.READY.name()).and();

            boolean useAbsoluteValues = false;
            if (diagrammTypeEnum.equals(DiagrammTypeEnum.CAKE_DIAGRAM)){
                useAbsoluteValues = true;
            }

            SearchFilter searchFilter = null;
            DiagrammDataChart diagrammDataChart = null;
            if (diagrammCategoryEnum.equals(DiagrammCategoryEnum.TIME_RANGE)){
                Integer maxPastMonth = 6;

                Calendar monthCalc = Calendar.getInstance();
                monthCalc.add(Calendar.MONTH, -1 * (maxPastMonth + 1));
                Integer lastDayOfMonthCalc = monthCalc.getActualMaximum(Calendar.DAY_OF_MONTH);
                Calendar lastDayCalc = (Calendar) monthCalc.clone();
                lastDayCalc.set(Calendar.DAY_OF_MONTH, lastDayOfMonthCalc);
                lastDayCalc.set(Calendar.HOUR_OF_DAY, 23);
                lastDayCalc.set(Calendar.MINUTE, 59);
                Date lastDayDateCalc = lastDayCalc.getTime();

                searchFilter = SearchHelper.searchAndUseSearchConfiguration(getApplicationContext(), true, sqlBuilder, lastDayDateCalc);
                diagrammDataChart = ChartHelper.chartForTimeRange(getApplicationContext(), searchFilter.getInvoiceCostDistributionItemList(), useAbsoluteValues, maxPastMonth);
            }else if (diagrammCategoryEnum.equals(DiagrammCategoryEnum.CATEGORY)){
                searchFilter = SearchHelper.searchAndUseSearchConfiguration(getApplicationContext(), true, sqlBuilder);
                diagrammDataChart = ChartHelper.chartForCategories(getApplicationContext(), searchFilter.getInvoiceCostDistributionItemList(), useAbsoluteValues);
            }else if (diagrammCategoryEnum.equals(DiagrammCategoryEnum.COST_PAYER)){
                searchFilter = SearchHelper.searchAndUseSearchConfiguration(getApplicationContext(), true, sqlBuilder);
                diagrammDataChart = ChartHelper.chartForCostPayer(getApplicationContext(), searchFilter.getInvoiceCostDistributionItemList(), useAbsoluteValues);
            }else if (diagrammCategoryEnum.equals(DiagrammCategoryEnum.PAYMENT_RECIPIENT)){
                searchFilter = SearchHelper.searchAndUseSearchConfiguration(getApplicationContext(), true, sqlBuilder);
                diagrammDataChart = ChartHelper.chartForPaymentRecipients(getApplicationContext(), searchFilter.getInvoiceCostDistributionItemList(), useAbsoluteValues);
            }else if (diagrammCategoryEnum.equals(DiagrammCategoryEnum.TIME_RANGE_DAYS)){
                searchFilter = SearchHelper.searchAndUseSearchConfiguration(getApplicationContext(), true, sqlBuilder);
                diagrammDataChart = ChartHelper.chartForDaysInMonth(getApplicationContext(), searchFilter.getInvoiceCostDistributionItemList(), useAbsoluteValues);
            }

            List<Object> results = new ArrayList<>();
            results.add(diagrammDataChart);
            results.add(searchFilter);

            return results;
        }

        @Override
        protected void onPostExecute(List<Object> objects) {

            DiagrammDataChart diagrammDataChart = (DiagrammDataChart) objects.get(0);
            SearchFilter searchFilter = (SearchFilter) objects.get(1);

            setFilterText(searchFilter.isShowSearchString(), searchFilter.getSearchString());

            if (diagrammTypeEnum.equals(DiagrammTypeEnum.LINE_DIAGRAM)){

                BarData barData = null;
                if (!diagrammDataChart.getCoordinates().isEmpty()){
                    barData = ChartHelper.convertDiagrammDataChartBeanToBarChart(diagrammDataChart);
                }

                XAxisGeneralFormatter axisGeneralFormatter = new XAxisGeneralFormatter(diagrammDataChart.getxAxesValues());

                XAxis xAxis = chart1.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setDrawGridLines(false);
                xAxis.setValueFormatter(axisGeneralFormatter);
                chart1.getAxisRight().setEnabled(false);

                chart2.setVisibility(View.GONE);

                xAxis.setGranularity(1f); // only intervals of 1 day
                xAxis.setLabelCount(4);
                chart1.getDescription().setEnabled(false);
                chart1.setData(barData);
                chart1.animateXY(500, 500);
                chart1.setVisibility(View.VISIBLE);
                chart1.invalidate();
            }else{
                PieData barData = null;
                if (!diagrammDataChart.getCoordinates().isEmpty()){
                    barData = ChartHelper.convertDiagrammDataChartBeanToPieChart(diagrammDataChart);
                }

                chart1.setVisibility(View.GONE);

                chart2.getDescription().setEnabled(false);
                chart2.setEntryLabelColor(Color.BLACK);
                chart2.setData(barData);
                chart2.setEntryLabelTextSize(14f);
                chart2.animateY(500, Easing.EasingOption.EaseInOutQuad);
                chart2.setVisibility(View.VISIBLE);
                chart2.invalidate();
            }

            analysisControlRl.setVisibility(View.VISIBLE);
            progressBarRl.setVisibility(View.INVISIBLE);

        }
    }
}
