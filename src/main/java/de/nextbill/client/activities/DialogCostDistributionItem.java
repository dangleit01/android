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

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.enums.CostDistributionItemTypeEnum;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.interfaces.CurrencyTextWatcher;
import de.nextbill.client.interfaces.CustomCurrencyTextWatcher;
import de.nextbill.client.model.CostDistributionItem;
import de.nextbill.client.model.IPaymentPerson;

public class DialogCostDistributionItem extends DialogFragment {

    private RadioButton costDistributionRadioButtonQuota;
    private RadioButton costDistributionRadioButtonFixedAmount;
    private RadioButton costDistributionRadioButtonPercent;
    private RadioButton costDistributionRadioButtonRest;
    private RadioGroup invoiceDetailsCostDistributionRadioGroup;
    private SeekBar seekBar;
    private EditText costDistributionFixedAmountEt;
    private TextView costDistributionQuotaTv;
    private TextView costDistributionPercentageTv;
    private TextView costDistributionTitleTv;
    private Button costDistributionOkButton;
    private EditText remarksEt;
    private RelativeLayout sumInputRl;
    private Switch costPaidSw;

    private CurrencyTextWatcher currencyTextWatcher;

    private BigDecimal currentValue;
    private BigDecimal moneyValue;

    private Boolean checkChange;

    private CostDistributionItem costDistributionItem;
    private CostDistributionItemTypeEnum costDistributionItemTypeEnum;

    private IPaymentPerson paymentPerson;
    private BigDecimal sum;
    private List<CostDistributionItem> costDistributionItems;
    private List<CostDistributionItem> costDistributionItemsWithoutCurrentItem;
    private String id;

    public void setArguments(IPaymentPerson paymentPerson, CostDistributionItem costDistributionItem, List<CostDistributionItem> costDistributionItems, BigDecimal sum, String id) {
        this.paymentPerson = paymentPerson;
        this.costDistributionItems = costDistributionItems;
        this.costDistributionItem = costDistributionItem;
        this.sum = sum;
        this.id = id;
    }

    public DialogCostDistributionItem() {
    }

    public interface EditCostDistributionDetailsDialogListener {
        void onFinishCostDistributionDetailsDialog(CostDistributionItem costDistributionItem, String id);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_cost_distribution_details, container);

        currentValue = costDistributionItem.getValue();
        costDistributionItemTypeEnum = costDistributionItem.getCostDistributionItemTypeEnum();
        costDistributionItemsWithoutCurrentItem = new ArrayList<CostDistributionItem>();
        costDistributionItemsWithoutCurrentItem.addAll(costDistributionItems);
        costDistributionItemsWithoutCurrentItem.remove(costDistributionItem);

        invoiceDetailsCostDistributionRadioGroup = (RadioGroup) view.findViewById(R.id.invoiceDetailsCostDistributionRadioGroup);

        costDistributionRadioButtonQuota = (RadioButton) view.findViewById(R.id.costDistributionRadioButtonQuota);
        costDistributionRadioButtonFixedAmount = (RadioButton) view.findViewById(R.id.costDistributionRadioButtonFixedAmount);
        costDistributionRadioButtonPercent = (RadioButton) view.findViewById(R.id.costDistributionRadioButtonPercent);
        costDistributionRadioButtonRest = (RadioButton) view.findViewById(R.id.costDistributionRadioButtonRest);
        remarksEt = (EditText) view.findViewById(R.id.remarksEt);

        costDistributionOkButton = (Button) view.findViewById(R.id.costDistributionOkButton);

        sumInputRl = (RelativeLayout) view.findViewById(R.id.sumInputRl);
        costDistributionFixedAmountEt = (EditText) view.findViewById(R.id.costDistributionFixedAmountEt);
        currencyTextWatcher = new CurrencyTextWatcher(costDistributionFixedAmountEt, currentValue);

        costDistributionTitleTv = (TextView) view.findViewById(R.id.costDistributionTitleTv);
        costDistributionQuotaTv = (TextView) view.findViewById(R.id.costDistributionQuotaTv);
        costDistributionPercentageTv = (TextView) view.findViewById(R.id.costDistributionPercentageTv);

        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        costPaidSw = (Switch) view.findViewById(R.id.costPaidSw);

        costDistributionTitleTv.setText(paymentPerson.getPaymentPersonName());
        remarksEt.setText(costDistributionItem.getRemarks());

        CustomCurrencyTextWatcher sumTextWatcher = new CustomCurrencyTextWatcher() {

            @Override
            public Boolean validate(BigDecimal newSum) {
                BigDecimal currentMaxAmount = CostDistributionHelper.getMaxAmountForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem, sum);
                if (newSum.compareTo(currentMaxAmount) == 1){
                    return false;
                }

                return true;
            }

            public void afterTextChanged(Editable s) {
                costDistributionFixedAmountEt.removeTextChangedListener(this);

                if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)){
                    currentValue = currencyTextWatcher.getResult();
                    moneyValue = currentValue;
                    updateCostDistributionItem();
                    refreshAllViews(CostDistributionItemTypeEnum.FIXED_AMOUNT);
                }

                costDistributionFixedAmountEt.addTextChangedListener(this);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };
        currencyTextWatcher.addCustomTextWatcher(sumTextWatcher);

        TextWatcher inputTextWatcher2 = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                costDistributionItem.setRemarks(s.toString());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        };

        remarksEt.addTextChangedListener(inputTextWatcher2);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if (b){
                    BigDecimal percent = new BigDecimal(i).divide(new BigDecimal(100), 30, RoundingMode.HALF_EVEN);

                    if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)) {
                        BigDecimal maxAmountForOneCostDistributionItem = CostDistributionHelper.getMaxAmountForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem, sum);
                        currentValue = maxAmountForOneCostDistributionItem.multiply(percent).setScale(2, RoundingMode.HALF_EVEN);
                    } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.QUOTA)) {
                        BigDecimal maxQuota = CostDistributionHelper.getCountWithoutFixed(costDistributionItems);
                        BigDecimal maxQuotaWeighted = CostDistributionHelper.getCountWeightedWithoutFixed(costDistributionItemsWithoutCurrentItem);
                        BigDecimal maxQuotaSubtractMaxQuotaWeighted = maxQuota.subtract(maxQuotaWeighted);

                        currentValue = percent.multiply(maxQuotaSubtractMaxQuotaWeighted).setScale(1, RoundingMode.HALF_EVEN);
                    } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.PERCENT)) {
                        BigDecimal maxAmount = CostDistributionHelper.getMaxAmountForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem, sum);
                        BigDecimal amountOfPercentOfMaxAmount = percent.multiply(maxAmount);
                        BigDecimal percentOfResult = amountOfPercentOfMaxAmount.divide(sum, 2, RoundingMode.HALF_EVEN);
                        currentValue = percentOfResult;
                    } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.REST)) {
                        currentValue = sum;
                    }

                    updateCostDistributionItem();
                    refreshAllViews(CostDistributionItemTypeEnum.UNDEFINED);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        costDistributionOkButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditCostDistributionDetailsDialogListener activity = (EditCostDistributionDetailsDialogListener) getActivity();

                updateCostDistributionItem();

                if (costDistributionItem != null) {
                    hideSoftInput();

                    activity.onFinishCostDistributionDetailsDialog(costDistributionItem, id);
                    DialogCostDistributionItem.this.dismiss();
                }
            }
        });

        costPaidSw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (checkChange == null || checkChange == true){
                    if (isChecked){
                        costDistributionItem.setCostPaid(moneyValue);
                    }else{
                        costDistributionItem.setCostPaid(BigDecimal.ZERO);
                    }

                    refreshCostPaidSw();
                }
            }
        });

        costDistributionRadioButtonQuota.setOnClickListener(new RadioGroup.OnClickListener() {
            @Override
            public void onClick(View view) {
                CostDistributionItemTypeEnum tmpCostDistributionItemTypeEnum = costDistributionItemTypeEnum;
                BigDecimal quota = CostDistributionHelper.calculateQuotaForValue(tmpCostDistributionItemTypeEnum, currentValue, costDistributionItemsWithoutCurrentItem, sum);
                currentValue = quota;
                activateRadioButtonAndUpdateEnum(CostDistributionItemTypeEnum.QUOTA);

                refreshAllViews(CostDistributionItemTypeEnum.QUOTA);

                hideSoftInput();
            }
        });

        costDistributionRadioButtonPercent.setOnClickListener(new RadioGroup.OnClickListener() {
            @Override
            public void onClick(View view) {
                CostDistributionItemTypeEnum tmpCostDistributionItemTypeEnum = costDistributionItemTypeEnum;
                BigDecimal percent = CostDistributionHelper.calculatePercentForValue(tmpCostDistributionItemTypeEnum, currentValue, costDistributionItemsWithoutCurrentItem, sum);
                currentValue = percent;
                activateRadioButtonAndUpdateEnum(CostDistributionItemTypeEnum.PERCENT);

                refreshAllViews(CostDistributionItemTypeEnum.PERCENT);

                hideSoftInput();
            }
        });

        costDistributionRadioButtonFixedAmount.setOnClickListener(new RadioGroup.OnClickListener() {
            @Override
            public void onClick(View view) {
                CostDistributionItemTypeEnum tmpCostDistributionItemTypeEnum = costDistributionItemTypeEnum;
                BigDecimal fixedAmount = CostDistributionHelper.calculateAmountForValue(tmpCostDistributionItemTypeEnum, currentValue, costDistributionItemsWithoutCurrentItem, sum);
                currentValue = fixedAmount;
                moneyValue = fixedAmount;
                activateRadioButtonAndUpdateEnum(CostDistributionItemTypeEnum.FIXED_AMOUNT);

                refreshAllViews(CostDistributionItemTypeEnum.FIXED_AMOUNT);
            }
        });

        costDistributionRadioButtonRest.setOnClickListener(new RadioGroup.OnClickListener() {
            @Override
            public void onClick(View view) {
                activateRadioButtonAndUpdateEnum(CostDistributionItemTypeEnum.REST);
                refreshAllViews(CostDistributionItemTypeEnum.REST);
            }
        });

        activateRadioButtonAndUpdateEnum(costDistributionItem.getCostDistributionItemTypeEnum());
        refreshAllViews(null);

        InputMethodManager inputMethodManager =
                (InputMethodManager) getActivity().getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && getActivity().getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    getActivity().getCurrentFocus().getWindowToken(), 0);
        }

        return view;

    }

    public void hideSoftInput(){
        InputMethodManager inputMethodManager =
                (InputMethodManager) getActivity().getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                costDistributionFixedAmountEt.getWindowToken(), 0);
        sumInputRl.requestFocus();
    }

    public void activateRadioButtonAndUpdateEnum(CostDistributionItemTypeEnum costDistributionItemTypeEnum) {
        if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.QUOTA)) {
            costDistributionRadioButtonFixedAmount.setChecked(false);
            costDistributionRadioButtonRest.setChecked(false);
            costDistributionRadioButtonPercent.setChecked(false);
            costDistributionRadioButtonQuota.setChecked(true);
            this.costDistributionItemTypeEnum = CostDistributionItemTypeEnum.QUOTA;
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)) {
            costDistributionRadioButtonQuota.setChecked(false);
            costDistributionRadioButtonRest.setChecked(false);
            costDistributionRadioButtonFixedAmount.setChecked(true);
            costDistributionRadioButtonPercent.setChecked(false);
            this.costDistributionItemTypeEnum = CostDistributionItemTypeEnum.FIXED_AMOUNT;
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.PERCENT)) {
            costDistributionRadioButtonQuota.setChecked(false);
            costDistributionRadioButtonRest.setChecked(false);
            costDistributionRadioButtonFixedAmount.setChecked(false);
            costDistributionRadioButtonPercent.setChecked(true);
            this.costDistributionItemTypeEnum = CostDistributionItemTypeEnum.PERCENT;
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.REST)) {
            costDistributionRadioButtonFixedAmount.setChecked(false);
            costDistributionRadioButtonQuota.setChecked(false);
            costDistributionRadioButtonRest.setChecked(true);
            costDistributionRadioButtonPercent.setChecked(false);
            this.costDistributionItemTypeEnum = CostDistributionItemTypeEnum.REST;
        }

        updateCostDistributionItem();
    }

    public void updateCostDistributionItem() {
        if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)) {
            costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.FIXED_AMOUNT);
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.QUOTA)) {
            costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.QUOTA);
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.PERCENT)) {
            costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.PERCENT);
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.REST)) {
            costDistributionItem.setCostDistributionItemTypeEnum(CostDistributionItemTypeEnum.REST);
        }

        costDistributionItem.setValue(currentValue);
    }

    public void refreshAllViews(CostDistributionItemTypeEnum costDistributionItemTypeEnum) {

        if (costDistributionItemTypeEnum != null) {
            if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)) {
                refreshProgressBar();
            } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.QUOTA)) {
                refreshFixedAmountEt();
                refreshProgressBar();
            } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.PERCENT)) {
                refreshFixedAmountEt();
                refreshProgressBar();
            } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.REST)) {
                refreshFixedAmountEt();
                refreshProgressBar();
            } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.UNDEFINED)) {
                refreshFixedAmountEt();
            }
        } else {
            refreshFixedAmountEt();
            refreshProgressBar();
        }

        refreshQuotaEt();
        refreshPercentageTv();

        refreshCostPaidSw();

    }

    public void refreshCostPaidSw() {
        BigDecimal costPaidTmp = this.costDistributionItem.getCostPaid() != null ? this.costDistributionItem.getCostPaid() : BigDecimal.ZERO;

        checkChange = false;

        if (costPaidTmp.compareTo(BigDecimal.ZERO) == 0) {
            costPaidSw.setChecked(false);
            costPaidSw.setText("Bezahlt");
        }else{
            costPaidSw.setChecked(true);
            costPaidSw.setText("Bezahlt (" + CurrencyTextWatcher.bigDecimalToString(costPaidTmp) + ")");
        }

        checkChange = true;
    }

    public void refreshPercentageTv() {
        BigDecimal percentOfCurrentQuota = new BigDecimal(0);
        if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)) {
            percentOfCurrentQuota = currentValue.divide(sum, 10, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.QUOTA)) {
            BigDecimal percentForValue = CostDistributionHelper.calculatePercentForValue(CostDistributionItemTypeEnum.QUOTA, currentValue, costDistributionItemsWithoutCurrentItem, sum);
            percentOfCurrentQuota = percentForValue.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.PERCENT)) {
            percentOfCurrentQuota = currentValue.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.REST)) {
            percentOfCurrentQuota = new BigDecimal(100);
        }

        costDistributionPercentageTv.setText(percentOfCurrentQuota.toString() + "%");
    }

    public void refreshQuotaEt() {
        BigDecimal quotaForEt = CostDistributionHelper.calculateQuotaForValue(costDistributionItemTypeEnum, currentValue, costDistributionItemsWithoutCurrentItem, sum);
        costDistributionQuotaTv.setText(quotaForEt.toString());
    }

    public void refreshFixedAmountEt() {
        BigDecimal fixedAmountForEt = CostDistributionHelper.calculateAmountForValue(costDistributionItemTypeEnum, currentValue, costDistributionItemsWithoutCurrentItem, sum);
        this.moneyValue = fixedAmountForEt;
        costDistributionFixedAmountEt.setText(CurrencyTextWatcher.bigDecimalToString(fixedAmountForEt));
    }

    public void refreshProgressBar() {

        BigDecimal percentForProgressEt = new BigDecimal(0);
        if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.FIXED_AMOUNT)) {
            BigDecimal maxAmountForOneCostDistributionItem = CostDistributionHelper.getMaxAmountForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem, sum);
            percentForProgressEt = currentValue.divide(maxAmountForOneCostDistributionItem, 10, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.QUOTA)) {
            BigDecimal maxQuota = CostDistributionHelper.getCountWithoutFixed(costDistributionItems);
            BigDecimal maxQuotaWeighted = CostDistributionHelper.getCountWeightedWithoutFixed(costDistributionItemsWithoutCurrentItem);
            BigDecimal maxQuotaSubtractMaxQuotaWeighted = maxQuota.subtract(maxQuotaWeighted);

            percentForProgressEt = currentValue.divide(maxQuotaSubtractMaxQuotaWeighted, 10, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);

//          BigDecimal amountForQuota = CostDistributionHelper.calculateAmountForValue(CostDistributionItemTypeEnum.QUOTA, currentValue, costDistributionItemsWithoutCurrentItem, sum);
//          BigDecimal percentOfCurrentAmountOfSum = amountForQuota.divide(sum, 10, RoundingMode.HALF_EVEN).multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN);
//          percentForProgressEt = percentOfCurrentAmountOfSum;
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.PERCENT)) {
            BigDecimal maxAmount = CostDistributionHelper.getMaxAmountForOneCostDistributionItem(costDistributionItemsWithoutCurrentItem, sum);
            BigDecimal amountOfPercent = currentValue.multiply(sum);
            BigDecimal percentOfResult = new BigDecimal(0);
            if (!maxAmount.equals(new BigDecimal(0))){
                percentOfResult  = amountOfPercent.divide(maxAmount, 3, RoundingMode.HALF_EVEN);
            }

            percentForProgressEt = percentOfResult.multiply(new BigDecimal(100));
        } else if (costDistributionItemTypeEnum.equals(CostDistributionItemTypeEnum.REST)) {
            percentForProgressEt = new BigDecimal(100);
        }

        seekBar.setProgress(percentForProgressEt.setScale(0, RoundingMode.HALF_EVEN).intValue());
    }

}
