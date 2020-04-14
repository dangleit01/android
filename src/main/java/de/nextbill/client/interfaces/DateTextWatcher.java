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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DateTextWatcher implements TextWatcher {
    private EditText editText;
    private CustomDateTextWatcher customTextWatcher;
    private static final String dayDigit = "t";
    private static final String monthDigit = "m";
    private static final String yearDigit = "j";
    private static final String standardText = dayDigit + dayDigit + "." + monthDigit + monthDigit + "." + yearDigit + yearDigit+ yearDigit +yearDigit;
    private static final String simpleDateFormatPattern = "dd.MM.yyyy";
    private Pattern dateRegex = Pattern.compile("[0-3+ "+dayDigit+"]{1}[0-9"+dayDigit+"]{1}[.][0-1"+monthDigit+
            "]{1}[0-9"+monthDigit+"]{1}[.][0-2"+yearDigit+"]{1}[0-9"+yearDigit+"9]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}");
    private Pattern dateOneDigitRegex = Pattern.compile("[0-9]{1}");

    private Pattern dateTooShortRegex1 = Pattern.compile("[0-9"+dayDigit+"]{1}[.][0-9"+monthDigit+"]{1}[0-9"+monthDigit+"]{1}[.][0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}");
    private Pattern dateTooShortRegex2 = Pattern.compile("[0-9"+dayDigit+"]{1}[0-9"+dayDigit+"]{1}[.]{1}[0-9"+monthDigit+"]{1}[.]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}");
    private Pattern dateTooShortRegex3 = Pattern.compile("[0-9"+dayDigit+"]{1}[0-9"+dayDigit+"]{1}[.]{1}[0-9"+monthDigit+"]{1}[0-9"+monthDigit+"]{1}[.]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}");
    private Pattern dateTooShortRegex4 = Pattern.compile("[0-3"+dayDigit+"]{1}[0-9"+dayDigit+"]{1}[0-1"+monthDigit+"]{1}[0-9"+monthDigit+"]{1}[.]{1}[0-2"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}");
    private Pattern dateTooShortRegex5 = Pattern.compile("[0-3"+dayDigit+"]{1}[0-9"+dayDigit+"]{1}[.]{1}[0-1"+monthDigit+"]{1}[0-9"+monthDigit+"]{1}[0-2"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}[0-9"+yearDigit+"]{1}");
    private String lastText;
    private String initialString;
    private Date lastDate;

    public DateTextWatcher(EditText editText, String initialDateString) {
        this.editText = editText;

        editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789."+dayDigit+monthDigit+yearDigit));
        editText.setSelectAllOnFocus(true);

        if (initialDateString != null){
            lastText = initialDateString;
            initialString = initialDateString;
            Date lastDateTmp = stringToDate(initialDateString);
            lastDate = lastDateTmp;

            editText.setText(initialDateString);
            editText.setSelection(0,editText.getText().toString().length());
        }else{
            lastText = standardText;
            initialString = standardText;
            editText.setText(lastText);
        }

        editText.addTextChangedListener(this);
    }

    public DateTextWatcher(EditText editText, Date initialDate) {
        this(editText, dateToString(initialDate));
    }

    public DateTextWatcher(EditText editText) {
        this(editText, standardText);
    }


    public static Date stringToDate(String dateString){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(simpleDateFormatPattern);
        Date parsedDate = null;
        try {
            parsedDate = simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
        }
        return parsedDate;
    }

    public static String dateToString(Date date){
        if (date == null){
            return null;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(simpleDateFormatPattern);
        String dateString = simpleDateFormat.format(date);
        return dateString;
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

        Matcher dateOneDigit = dateOneDigitRegex.matcher(text);

        if (dateOneDigit.matches()){
            String givenDigit = text.substring(0,1);
            String subStringOfLastText = lastText.substring(1, lastText.length());
            s.clear();
            s.append(givenDigit + subStringOfLastText);
            editText.setSelection(1);
        }else if (text.equals("")){
            s.clear();
            s.append(initialString);
            editText.setSelection(0);
        }

        Matcher dateRegexMatcher = dateRegex.matcher(text);
        if (!dateRegexMatcher.matches()){
            Integer startSelection = editText.getSelectionStart();

            boolean customSelection = false;

            Matcher dateTooShortMatcher1 = dateTooShortRegex1.matcher(text);
            Matcher dateTooShortMatcher2 = dateTooShortRegex2.matcher(text);
            Matcher dateTooShortMatcher3 = dateTooShortRegex3.matcher(text);
            Matcher dateTooShortMatcher4 = dateTooShortRegex4.matcher(text);
            Matcher dateTooShortMatcher5 = dateTooShortRegex5.matcher(text);

            if (dateTooShortMatcher1.matches()){
                s.replace(startSelection, startSelection, dayDigit);
            }else if (dateTooShortMatcher2.matches()){
                s.replace(startSelection, startSelection, monthDigit);
                if (startSelection == 3){
                    editText.setSelection(startSelection-1);
                    customSelection = true;
                }
            }else if (dateTooShortMatcher3.matches()){
                s.replace(startSelection, startSelection, yearDigit);
                if (startSelection == 6){
                    editText.setSelection(startSelection-1);
                    customSelection = true;
                }
            }else if (dateTooShortMatcher4.matches()){
                s.replace(startSelection, startSelection, ".");

            }else if (dateTooShortMatcher5.matches()){
                s.replace(startSelection, startSelection, ".");
            }

            String text2 = editText.getText().toString();
            Matcher dateRegexMatcher2 = dateRegex.matcher(text2);
            if (!dateRegexMatcher2.matches()){

                if (startSelection == text2.length()){
                    s.replace(startSelection -1, startSelection, "");
                }else{
                    if (startSelection == 3 || startSelection == 6){
                        String subStringOfLastText = text2.substring(startSelection-1, startSelection);
                        s.replace(startSelection-1, startSelection + 2, "."+subStringOfLastText);
                        editText.setSelection(startSelection+1);
                    }else{
                        s.replace(startSelection, startSelection + 1, "");
                    }
                }

                String text3 = editText.getText().toString();
                Matcher dateRegexMatcher3 = dateRegex.matcher(text3);

                if (!dateRegexMatcher3.matches()){
                    s.clear();
                    s.append(lastText);
                    if (startSelection == 0){
                        editText.setSelection(startSelection);
                    }else{
                        editText.setSelection(startSelection-1);
                    }
                }else{
                    lastText = text3;
                    if (startSelection == 2 || startSelection == 5){
                        editText.setSelection(startSelection+1);
                    }
                }

            }else{
                lastText = text2;
                if (!customSelection){
                    editText.setSelection(startSelection);
                }
            }
        }

        this.lastDate = stringToDate(lastText);

        if (customTextWatcher != null){
            customTextWatcher.currentValue(lastDate);
        }

        editText.addTextChangedListener(this);

        if (customTextWatcher != null){
            customTextWatcher.afterTextChanged(s);
        }
    }

    public void setResult(String initialDateString){
        if (initialDateString != null){
            lastText = initialDateString;
            initialString = initialDateString;
            Date lastDateTmp = stringToDate(initialDateString);
            lastDate = lastDateTmp;

            editText.setText(initialDateString);
        }else{
            lastText = standardText;
            initialString = standardText;
            editText.setText(lastText);
        }
    }

    public void reset(){
        lastText = standardText;
        initialString = standardText;
        editText.setText(lastText);
    }

    public void setResult(Date initialDate){
        setResult(dateToString(initialDate));
    }

    public Date getResult(){
        return lastDate;
    }

    public void addCustomTextWatcher(CustomDateTextWatcher textWatcher){
        customTextWatcher = textWatcher;
    }
}
