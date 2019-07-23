package tiratom.techacademy.taskapp

import android.R
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*


const val EXTRA_TASK = "jp.tiratom.techacademy.taskapp.Task"

class MainActivity : AppCompatActivity() {

    private lateinit var mRealm: Realm
    private val mRealmListner = object: RealmChangeListener<Realm>{
        override fun onChange(element: Realm) {
            reloadTaskView()
        }

    }

    private lateinit var mTaskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListner)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        // ListViewをタップした時の処理
        listView1.setOnItemClickListener{ parent, _, position, _ ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押しした時の処理
        listView1.setOnItemLongClickListener { parent, view, position, id ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)


            builder.setTitle("削除")
            builder.setMessage("${task.title}を削除しますか？")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                reloadTaskView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }


        // TODO 検索バーの設定


        reloadTaskView()
    }




    private fun reloadTaskView(){

        lateinit var taskRealmResults: RealmResults<Task>

        if (intent.action == Intent.ACTION_SEARCH) {
            val searchWord = intent.getStringExtra(SearchManager.QUERY)?.also { query ->
            taskRealmResults = mRealm.where(Task::class.java).equalTo("category", query).findAll().sort("date", Sort.DESCENDING)
            }
        } else {
            // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
            taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
        }


        // 上記の結果をTaskListとしてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults)

        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するため、アダプタにデータ更新通知を行う
        mTaskAdapter.notifyDataSetChanged()

    }


    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }
}
