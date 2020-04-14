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

package de.nextbill.client.interfaces;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CurrencyTextWatcher implements TextWatcher{

    private EditText editText;
    private BigDecimal currentSum = new BigDecimal(0.0);
    private CustomCurrencyTextWatcher customTextWatcher;

    private Pattern currencyRegex00 = Pattern.compile("[0-9]{0,1}");
    private Pattern currencyRegex0 = Pattern.compile("");
    private Pattern currencyRegex1 = Pattern.compile("[0]{1},[0]{1}[0]{1}[0-9]{0,1}");
    private Pattern currencyRegex2 = Pattern.compile("[0]{1},[0]{1}[0-9]{1}[0-9]{1}");
    private Pattern currencyRegex3 = Pattern.compile("[0]{1},[0-9]{1}[0-9]{1}[0-9]{1}");
    private Pattern currencyRegex4 = Pattern.compile("[0-9]+,[0-9]{1}[0-9]{1}[0-9]{1}");
    private Pattern currencyRegex5 = Pattern.compile("[0-9]+,[0-9]{1}");
    private Pattern currencyRegex6 = Pattern.compile("[0-9]{1},[0-9]{1}");
    private Pattern currencyRegex7 = Pattern.compile("[0-9]+,[0-9]{1}[0-9]{1}");

    private String oldString = "0,00";

    public static String bigDecimalToString(BigDecimal currentDecimal){
        NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.GERMAN);
        numberFormatter.setGroupingUsed(false);
        numberFormatter.setMaximumFractionDigits(2);
        numberFormatter.setMinimumFractionDigits(2);

        String number = numberFormatter.format(currentDecimal);
        return number;
    }

    public CurrencyTextWatcher(EditText editText, BigDecimal currentSum) {
        this.editText = editText;
        editText.addTextChangedListener(this);
        editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789,"));
        editText.setSelectAllOnFocus(true);

        if (currentSum != null){
            this.currentSum = currentSum;

            editText.setText(bigDecimalToString(currentSum));
            editText.setSelection(0,editText.getText().toString().length());
        }else{
            editText.setText("0,00");
        }
    }

    public CurrencyTextWatcher(EditText editText) {
        this(editText, null);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (customTextWatcher != null){
            customTextWatcher.beforeTextChanged(s, start, count, after);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (customTextWatcher != null){
            customTextWatcher.onTextChanged(s, start, before, count);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        editText.removeTextChangedListener(this);

        String text = s.toString();

        Matcher currencyRegex00Matcher = currencyRegex00.matcher(text);
        Matcher currencyRegex0Matcher = currencyRegex0.matcher(text);
        Matcher currencyRegex1Matcher = currencyRegex1.matcher(text);
        Matcher currencyRegex2Matcher = currencyRegex2.matcher(text);
        Matcher currencyRegex3Matcher = currencyRegex3.matcher(text);
        Matcher currencyRegex4Matcher = currencyRegex4.matcher(text);
        Matcher currencyRegex5Matcher = currencyRegex5.matcher(text);
        Matcher currencyRegex6Matcher = currencyRegex6.matcher(text);
        Matcher currencyRegex7Matcher = currencyRegex7.matcher(text);

        if (currencyRegex00Matcher.matches()) {
            String number = s.subSequence(s.length()-1,s.length()).toString();
            s.replace(0, s.length(), "0,0" +number);
        }else if (currencyRegex0Matcher.matches()){
            s.replace(0, s.length(), "0,00");
        }else if (currencyRegex1Matcher.matches()){
            s.replace(3, 4, "");
        }else if (currencyRegex2Matcher.matches()){
            s.replace(2, 3, "");
        }else if (currencyRegex3Matcher.matches()){
            String numberWithComma = s.subSequence(2,3).toString()+",";
            s.replace(0, 3, numberWithComma);
        }else if (currencyRegex4Matcher.matches()){
            String numberAfterComma = s.subSequence(s.length()-3,s.length()-2).toString();
            s.replace(s.length() - 4, s.length() - 2, numberAfterComma + ",");
        }else if (currencyRegex6Matcher.matches()){
            String numberAfterComma = s.subSequence(s.length()-3,s.length()-2).toString();
            s.replace(s.length() - 3, s.length() - 1, "0," + numberAfterComma);
        }else if (currencyRegex5Matcher.matches()) {
            String numberAfterComma = s.subSequence(s.length() - 3, s.length() - 2).toString();
            s.replace(s.length() - 3, s.length() - 1, "," + numberAfterComma);
        }else{

            String testString = s.toString();
            String doubleTestString = testString.replace(",", ".");
            boolean resetString = false;
            try{
                Double.valueOf(doubleTestString);
            }catch (NumberFormatException e){
                resetString = true;
            }

            if (!currencyRegex7Matcher.matches()) {
                s.replace(s.length()-1, s.length(), "");
                Matcher currencyRegex8Matcher = currencyRegex7.matcher(s.toString());
                if (!currencyRegex8Matcher.matches()) {
                    resetString = true;
                }
            }

            if (resetString){
                s.clear();
                s.append(oldString);
            }
        }

        Double sumDouble = 0.0;
        try{
            sumDouble = Double.valueOf(s.toString().replace(",","."));
        }catch (NumberFormatException e){

        }
        BigDecimal tmpCurrentSum = new BigDecimal(sumDouble).setScale(2, BigDecimal.ROUND_HALF_EVEN);

        Boolean isValid = true;
        if (customTextWatcher != null){
            isValid = customTextWatcher.validate(tmpCurrentSum);
        }

        if (isValid != null && !isValid){
            s.clear();
            s.append(oldString);
        }else{
            oldString = s.toString();
            currentSum = tmpCurrentSum;
        }

        editText.addTextChangedListener(this);

        if (customTextWatcher != null){
            customTextWatcher.afterTextChanged(s);
        }
    }

    public BigDecimal getResult(){
        return currentSum;
    }

    public void setSum(BigDecimal currentSum){
        if (currentSum != null){
            this.currentSum = currentSum;

            editText.setText(bigDecimalToString(currentSum));
        }else{
            editText.setText("0,00");
        }
    }

    public void addCustomTextWatcher(CustomCurrencyTextWatcher textWatcher){
        customTextWatcher = textWatcher;
    }
}
