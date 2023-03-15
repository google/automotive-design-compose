/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose.reference.homecompose

import android.app.Activity
import android.car.Car
import android.car.VehicleAreaType
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.CarPropertyManager.CarPropertyEventCallback
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

// import com.android.designcompose.reference.figma.generated.CenterDisplay;
/// This class copies various interesting vehicle properties from the CarPropertyManager
/// into the document.
class CarProperties(activity: Activity?) : CarPropertyEventCallback {
  private val mCarPropertyManager: CarPropertyManager
  private var mBatteryLevel: Float = 0F
  private var mBatteryCapacity: Float = 0F
  private var mRangeRemaining: Float = 0F
  private var mOdometer: Float = 0F
  private var mSetProps: (CarPropertyStrings) -> Unit = {}

  internal class CarPropertyStrings(
    var range: String = "",
    var charge: String = "",
    var odometer: String = "",
  )

  init {
    val car = Car.createCar(activity)
    mCarPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
    getCurrentProperties()
  }

  private fun getCurrentProperties() {
    mBatteryLevel =
      mCarPropertyManager.getFloatProperty(
        VehiclePropertyIds.EV_BATTERY_LEVEL,
        VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
      )
    mCarPropertyManager.registerCallback(
      this,
      VehiclePropertyIds.EV_BATTERY_LEVEL,
      CarPropertyManager.SENSOR_RATE_UI
    )
    mBatteryCapacity =
      mCarPropertyManager.getFloatProperty(
        VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,
        VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
      )
    mCarPropertyManager.registerCallback(
      this,
      VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,
      CarPropertyManager.SENSOR_RATE_UI
    )
    mRangeRemaining =
      mCarPropertyManager.getFloatProperty(
        VehiclePropertyIds.RANGE_REMAINING,
        VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
      )
    mCarPropertyManager.registerCallback(
      this,
      VehiclePropertyIds.RANGE_REMAINING,
      CarPropertyManager.SENSOR_RATE_UI
    )
    mOdometer =
      mCarPropertyManager.getFloatProperty(
        VehiclePropertyIds.PERF_ODOMETER,
        VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
      )
    mCarPropertyManager.registerCallback(
      this,
      VehiclePropertyIds.PERF_ODOMETER,
      CarPropertyManager.SENSOR_RATE_UI
    )
  }

  override fun onChangeEvent(value: CarPropertyValue<*>) {
    var changed = false
    when (value.propertyId) {
      VehiclePropertyIds.EV_BATTERY_LEVEL -> {
        val batteryLevel = value.value as Float
        if (mBatteryLevel != batteryLevel) {
          mBatteryLevel = batteryLevel
          changed = true
        }
      }
      VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY -> {
        val batteryCapacity = value.value as Float
        if (mBatteryCapacity != batteryCapacity) {
          mBatteryCapacity = batteryCapacity
          changed = true
        }
      }
      VehiclePropertyIds.RANGE_REMAINING -> {
        val rangeRemaining = value.value as Float
        if (mRangeRemaining != rangeRemaining) {
          mRangeRemaining = rangeRemaining
          changed = true
        }
      }
      VehiclePropertyIds.PERF_ODOMETER -> {
        val odometer = value.value as Float
        if (mOdometer != odometer) {
          mOdometer = odometer
          changed = true
        }
      }
    }

    if (changed) {
      val props = getCarPropertyStrings()
      mSetProps(props)
    }
  }

  override fun onErrorEvent(propId: Int, zone: Int) {
    Log.e("DesignCompose", "CarProperties error event for $propId in zone $zone")
  }

  private fun getCarPropertyStrings(): CarPropertyStrings {
    val range = String.format("%.0f", mRangeRemaining / 1000.0)
    val charge = String.format("%.0f", mBatteryLevel * 100.0 / mBatteryCapacity)
    val odometer = String.format("%.0f", mOdometer)
    return CarPropertyStrings(range, charge, odometer)
  }

  @Composable
  internal fun getProperties(): CarPropertyStrings {
    val (props, setProps) =
      remember {
        getCurrentProperties()
        val carPropertyStrings = getCarPropertyStrings()
        mutableStateOf(carPropertyStrings)
      }
    LaunchedEffect(Unit) { mSetProps = setProps }
    return props
  }
}
