package jp.techacademy.naoya.konita.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmConfiguration
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var mRealm: Realm
    private val mRealmListener = object: RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }
    private lateinit var mTaskAdapter: TaskAdapter

    val EXTRA_TASK = "jp.techacademy.naoya.konita.taskapp.TASK"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        //val config = RealmConfiguration.Builder(this).deleteRealmIfMigrationNeeded().build()
        //Realm.setDefaultConfiguration(config)

        val config0 = RealmConfiguration.Builder()
            .name("default0.realm")
            .schemaVersion(3)
            .build()

        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this)

        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK") {_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            filter_button.setOnClickListener { view ->
                filterListView()
            }

            true
        }

        reloadListView()
    }

    private fun reloadListView() {
        // Realmデータベースから、「すべてのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)

        // 上記の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    private fun filterListView() {
        // Realmデータベースから、「カテゴリー」でフィルターする
        val taskRealmResults = mRealm.where(Task::class.java).contains("category", filter_text.text.toString()).findAll().sort("date", Sort.DESCENDING)

        // 上記の結果を、TaskListとしてセットする
        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

    private fun addTaskForTest() {
        val task = Task()
        task.title = "作業"
        task.contents = "プログラムを書いてPUSHする"
        task.date = Date()
        task.id = 0
        mRealm.beginTransaction()
        mRealm.copyToRealmOrUpdate(task)
        mRealm.commitTransaction()
    }
}