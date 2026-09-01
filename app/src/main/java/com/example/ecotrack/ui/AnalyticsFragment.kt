package com.example.ecotrack.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.ecotrack.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class AnalyticsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)
        val barChart = view.findViewById<BarChart>(R.id.barChart)
        val tvTitle = view.findViewById<TextView>(R.id.tvAnalyticsTitle)

        // Set specific dark grey color for interpretation words
        val darkGrey = Color.parseColor("#616161")
        tvTitle.setTextColor(darkGrey)

        viewModel.allLogs.observe(viewLifecycleOwner) { logs ->
            val weeklyData = viewModel.getWeeklyStepData(logs)
            updateChart(barChart, weeklyData, darkGrey)
        }

        return view
    }

    private fun updateChart(barChart: BarChart, dataPoints: List<Pair<String, Float>>, textColor: Int) {
        val entries = dataPoints.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second)
        }

        val dataSet = BarDataSet(entries, "Steps Walked").apply {
            color = Color.parseColor("#4CAF50")
            valueTextColor = textColor
            valueTextSize = 10f
        }

        val labels = dataPoints.map { it.first }.toTypedArray()
        
        barChart.apply {
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true
                this.textColor = textColor
                setDrawGridLines(false)
                axisLineColor = textColor
            }
            
            axisLeft.apply {
                this.textColor = textColor
                axisLineColor = textColor
                gridColor = Color.LTGRAY
            }
            
            axisRight.isEnabled = false
            legend.apply {
                this.textColor = textColor
                isEnabled = true
            }
            
            description.isEnabled = false
            setNoDataTextColor(textColor)
            
            data = BarData(dataSet)
            animateY(1000)
            invalidate()
        }
    }
}
