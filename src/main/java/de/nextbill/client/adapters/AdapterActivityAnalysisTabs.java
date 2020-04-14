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

package de.nextbill.client.adapters;

import android.content.Context;
import android.util.SparseArray;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import de.nextbill.client.activities.ActivityAnalysisTabTimeRange;

public class AdapterActivityAnalysisTabs extends FragmentStatePagerAdapter {

    private final SparseArray<WeakReference<Fragment>> instantiatedFragments = new SparseArray<>();
    private ArrayList<String> mTabHeader;
    private Context context;

    public AdapterActivityAnalysisTabs(Context context, FragmentManager fm, ArrayList<String> tabHeader) {
        super(fm);
        this.mTabHeader = tabHeader;
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return Fragment.instantiate(context, ActivityAnalysisTabTimeRange.class.getName());
            default:
                return null;
        }
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position) {
        final Fragment fragment = (Fragment) super.instantiateItem(container, position);
        instantiatedFragments.put(position, new WeakReference<>(fragment));
        return fragment;
    }

    @Override
    public void destroyItem(final ViewGroup container, final int position, final Object object) {
        instantiatedFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    @Override
    public int getCount() {
        return mTabHeader.size();
    }


    public Fragment getFragment(final int position, ViewGroup container) {
        final WeakReference<Fragment> wr = instantiatedFragments.get(position);
        if (wr != null) {
            return wr.get();
        } else {
            return (Fragment) instantiateItem(container, position);
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabHeader.get(position);
    }

    public ArrayList<String> getmTabHeader() {
        return mTabHeader;
    }

    public void setmTabHeader(ArrayList<String> mTabHeader) {
        this.mTabHeader = mTabHeader;
    }
}
