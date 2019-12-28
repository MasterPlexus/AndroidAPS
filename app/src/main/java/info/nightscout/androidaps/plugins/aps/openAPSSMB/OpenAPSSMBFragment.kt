package info.nightscout.androidaps.plugins.aps.openAPSSMB

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.logging.L
import info.nightscout.androidaps.plugins.aps.openAPSMA.events.EventOpenAPSUpdateGui
import info.nightscout.androidaps.plugins.aps.openAPSMA.events.EventOpenAPSUpdateResultGui
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.utils.DateUtil
import info.nightscout.androidaps.utils.FabricPrivacy
import info.nightscout.androidaps.utils.JSONFormatter
import info.nightscout.androidaps.utils.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.openapsama_fragment.*
import org.json.JSONArray
import org.json.JSONException
import org.slf4j.LoggerFactory

class OpenAPSSMBFragment : Fragment() {
    private lateinit var mRunnable:Runnable
    private lateinit var mHandler: Handler
    private val log = LoggerFactory.getLogger(L.APS)
    private var disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.openapsama_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh_openaps_ama.setColorSchemeResources(R.color.orange, R.color.green, R.color.blue)
        swipeRefresh_openaps_ama.setProgressBackgroundColorSchemeColor(ResourcesCompat.getColor(resources, R.color.swipe_background, null))
        // Initialize the handler instance
        mHandler = Handler()

        swipeRefresh_openaps_ama.setOnRefreshListener {

            mRunnable = Runnable {
                // Hide swipe to refresh icon animation
                swipeRefresh_openaps_ama.isRefreshing = false
                OpenAPSSMBPlugin.getPlugin().invoke("OpenAPSMB button", false)
            }

            // Execute the task after specified time
            mHandler.postDelayed(
                mRunnable,
                (3000).toLong() // Delay 1 to 5 seconds
            )
        }

    }

    @Synchronized
    override fun onResume() {
        super.onResume()
        disposable += RxBus
                .toObservable(EventOpenAPSUpdateGui::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateGUI()
                }, {
                    FabricPrivacy.logException(it)
                })
        disposable += RxBus
                .toObservable(EventOpenAPSUpdateResultGui::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateResultGUI(it.text)
                }, {
                    FabricPrivacy.logException(it)
                })

        updateGUI()
    }

    @Synchronized
    override fun onPause() {
        super.onPause()
        disposable.clear()
    }

    @Synchronized
    fun updateGUI() {
        if (openapsma_result == null) return
        val plugin = OpenAPSSMBPlugin.getPlugin()
        plugin.lastAPSResult?.let { lastAPSResult ->
            openapsma_result.text = JSONFormatter.format(lastAPSResult.json)
            openapsma_request.text = lastAPSResult.toSpanned()
        }
        plugin.lastDetermineBasalAdapterSMBJS?.let { determineBasalAdapterSMBJS ->
            openapsma_glucosestatus.text = JSONFormatter.format(determineBasalAdapterSMBJS.glucoseStatusParam)
            openapsma_currenttemp.text = JSONFormatter.format(determineBasalAdapterSMBJS.currentTempParam)
            try {
                val iobArray = JSONArray(determineBasalAdapterSMBJS.iobDataParam)
                openapsma_iobdata.text = TextUtils.concat(String.format(MainApp.gs(R.string.array_of_elements), iobArray.length()) + "\n", JSONFormatter.format(iobArray.getString(0)))
            } catch (e: JSONException) {
                log.error("Unhandled exception", e)
                @SuppressLint("SetTextl18n")
                openapsma_iobdata.text = "JSONException see log for details"
            }

            openapsma_profile.text = JSONFormatter.format(determineBasalAdapterSMBJS.profileParam)
            openapsma_mealdata.text = JSONFormatter.format(determineBasalAdapterSMBJS.mealDataParam)
            openapsma_scriptdebugdata.text = determineBasalAdapterSMBJS.scriptDebug
            plugin.lastAPSResult?.inputConstraints?.let {
                openapsma_constraints.text = it.reasons
            }
        }
        if (plugin.lastAPSRun != 0L) {
            openapsma_lastrun.text = DateUtil.dateAndTimeFullString(plugin.lastAPSRun)
        }
        plugin.lastAutosensResult?.let {
            openapsma_autosensdata.text = JSONFormatter.format(it.json())
        }
    }

    @Synchronized
    private fun updateResultGUI(text: String) {
        if (openapsma_result == null) return
        openapsma_result.text = text
        openapsma_glucosestatus.text = ""
        openapsma_currenttemp.text = ""
        openapsma_iobdata.text = ""
        openapsma_profile.text = ""
        openapsma_mealdata.text = ""
        openapsma_autosensdata.text = ""
        openapsma_scriptdebugdata.text = ""
        openapsma_request.text = ""
        openapsma_lastrun.text = ""
    }
}
