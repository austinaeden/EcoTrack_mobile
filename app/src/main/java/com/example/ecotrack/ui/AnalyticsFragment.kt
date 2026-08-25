package com.example.ecotrack.ui

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.fragment.app.Fragment
import com.example.ecotrack.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class AnalyticsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)
        val barChart = view.findViewById<BarChart>(R.id.barChart)

        setupChart(barChart)
        return view
    }

    private fun setupChart(barChart: BarChart) {
        val textColor = getThemeColor(android.R.attr.textColorPrimary)

        val entries = arrayListOf(
            BarEntry(0f, 4200f),
            BarEntry(1f, 6800f),
            BarEntry(2f, 8500f),
            BarEntry(3f, 5100f),
            BarEntry(4f, 9200f),
            BarEntry(5f, 11000f),
            BarEntry(6f, 7400f)
        )

        val dataSet = BarDataSet(entries, "Steps Walked").apply {
            color = Color.parseColor("#4CAF50")
            valueTextColor = textColor
            valueTextSize = 12f
        }

        val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        barChart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(days)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true
                this.textColor = textColor
                setDrawGridLines(false)
            }
            
            axisLeft.textColor = textColor
            axisRight.isEnabled = false
            legend.textColor = textColor
            description.isEnabled = false
            
            data = BarData(dataSet)
            animateY(1000)
            invalidate()
        }
    }

    private fun getThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
