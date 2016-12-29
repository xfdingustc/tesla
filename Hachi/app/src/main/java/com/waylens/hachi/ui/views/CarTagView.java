package com.waylens.hachi.ui.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.rest.bean.VehicleInfo;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Xiaofei on 2016/12/29.
 */

public class CarTagView extends LinearLayout {
    private static final String TAG = CarTagView.class.getSimpleName();
    @BindView(R.id.car_mark)
    ImageView ivCarMark;

    @BindView(R.id.car_model)
    TextView tvCarModel;

    public CarTagView(Context context) {
        this(context, null);
    }

    public CarTagView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.view_car_tag, this, true);
        setBackgroundResource(R.drawable.race_info_bg);
        ButterKnife.bind(this);
    }

    public void load(VehicleInfo vehicleInfo) {
        int markRes = CarMarkHelper.getCarMark(vehicleInfo.vehicleMaker);
        Logger.t(TAG).d("mark Res: " + markRes);
        if (markRes != -1) {
            ivCarMark.setImageResource(markRes);
            tvCarModel.setText(vehicleInfo.toString());
        } else {
            ivCarMark.setImageResource(R.drawable.ic_race_car);
            tvCarModel.setText(vehicleInfo.toString());
        }
    }

    /**
     * Created by Xiaofei on 2016/11/27.
     */

    private  static class CarMarkHelper {

        private static final CarMark[] mListedCar = new CarMark[]{
            new CarMark("land rover", R.drawable.ic_landrover),
            new CarMark("chevrolet", R.drawable.ic_chevrolet),
            new CarMark("jeep", R.drawable.ic_jeep),
            new CarMark("mercedes-benz", R.drawable.ic_bens),
            new CarMark("mitsubishi", R.drawable.ic_mitsubishi),
            new CarMark("ford", R.drawable.ic_ford),
            new CarMark("honda", R.drawable.ic_honda),
            new CarMark("subaru", R.drawable.ic_subaru_logo_and_wordmark),
            new CarMark("ferrari", R.drawable.ic_ferrari),
            new CarMark("bmw", R.drawable.ic_bmw2),
            new CarMark("kia", R.drawable.ic_kia),
            new CarMark("infiniti", R.drawable.ic_infiniti),
            new CarMark("toyota", R.drawable.ic_toyota),
            new CarMark("volkswagen", R.drawable.ic_volkswagen),
            new CarMark("tesla", R.drawable.ic_tesla),
            new CarMark("audi", R.drawable.ic_audi),
            new CarMark("acura", R.drawable.ic_acura),
            new CarMark("mazda", R.drawable.ic_mazda),
            new CarMark("nissan", R.drawable.ic_nissan),
            new CarMark("jaguar", R.drawable.ic_jaguar),
            new CarMark("dodge", R.drawable.ic_dodge),
            new CarMark("ram", R.drawable.ic_dodge)


        };

        public static int getCarMark(String maker) {
            for (int i = 0; i < mListedCar.length; i++) {
                CarMark carMark = mListedCar[i];
                if (!TextUtils.isEmpty(maker) && maker.toLowerCase().equals(carMark.maker)) {
                    return carMark.markRes;
                }
            }

            return -1;
        }


        public static class CarMark {
            String maker;
            int markRes;

            public CarMark(String maker, int markRes) {
                this.maker = maker;
                this.markRes = markRes;
            }
        }
    }

}
