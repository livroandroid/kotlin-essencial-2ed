package br.com.livroandroid.carros.fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import br.com.livroandroid.carros.R
import br.com.livroandroid.carros.activity.CarroActivity
import br.com.livroandroid.carros.adapter.CarroAdapter
import br.com.livroandroid.carros.domain.Carro
import br.com.livroandroid.carros.domain.CarroService
import br.com.livroandroid.carros.domain.TipoCarro
import br.com.livroandroid.carros.domain.event.SaveCarroEvent
import br.com.livroandroid.carros.extensions.toast


import kotlinx.android.synthetic.main.fragment_carros.*
import kotlinx.android.synthetic.main.include_progress.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.startActivity

import java.util.concurrent.Callable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
open class CarrosFragment : BaseFragment() {
    private var tipo = TipoCarro.classicos
    protected var carros = listOf<Carro>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            tipo = arguments?.getSerializable("tipo") as TipoCarro
        }
        // Registra para receber eventos do bus
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Retorna a view /res/layout/fragment_carros.xml
        return inflater.inflate(R.layout.fragment_carros, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Views
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.setHasFixedSize(true)

        // Swipe to Refresh
        swipeToRefresh.setOnRefreshListener {
            taskCarros()
        }
        swipeToRefresh.setColorSchemeResources(
                R.color.refresh_progress_1,
                R.color.refresh_progress_2,
                R.color.refresh_progress_3)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        taskCarros()
    }

    @SuppressLint("CheckResult")
    open fun taskCarros() {
        progress.visibility = View.VISIBLE

        Observable.fromCallable { CarroService.getCarros(tipo) } // busca os carros
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( {
                /** onNext **/
                recyclerView.adapter = CarroAdapter(it) { onClickCarro(it) }

                progress.visibility = View.INVISIBLE
                swipeToRefresh.isRefreshing = false

                toast("BOOM RX!")
            },{
                /** onError **/
                toast("Ocorreu um erro!")
            })

        Observable.fromCallable { CarroService.getCarros(tipo) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { /* onNext */ },
                { /* onError */ },
                { /* onComplete */ },
                { /* onSubscribe */ }
            )


        Observable.fromCallable(object: Callable<List<Carro>> {
            override fun call(): List<Carro> {
                return CarroService.getCarros(tipo)
            }

        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object: Consumer<List<Carro>> {
                // Implementação do onNext
                override fun accept(carros: List<Carro>) {
                    recyclerView.adapter = CarroAdapter(carros) { onClickCarro(it) }

                    progress.visibility = View.INVISIBLE
                    swipeToRefresh.isRefreshing = false
                }

            }, object: Consumer<Throwable> {
                // Implementação do onError
                override fun accept(t: Throwable?) {
                    toast("Erro ao consultar o servidor: $t")

                    progress.visibility = View.INVISIBLE
                    swipeToRefresh.isRefreshing = false
                }
            })

    }

    /*open fun taskCarros() {
        // Abre uma thread
        doAsync {
            // Busca os carros
            carros = CarroService.getCarros(tipo)
            // Atualiza a lista na UI Thread
            uiThread {
                recyclerView.adapter = CarroAdapter(carros) { onClickCarro(it) }

                // Termina a animação do Swipe to Refresh
                swipeToRefresh.isRefreshing = false
            }
        }
    }*/

    /*open fun taskCarros() {
        object : Thread() {
            override fun run() {
                // Busca os carros
                carros = CarroService.getCarros(tipo)
                // Atualiza a lista
                activity?.runOnUiThread(Runnable {
                    // Atualiza a lista
                    recyclerView.adapter = CarroAdapter(carros) { onClickCarro(it) }

                })l
            }
        }.start()
    }*/


    open fun onClickCarro(carro: Carro) {
        activity?.startActivity<CarroActivity>("carro" to carro)
    }

    @Subscribe
    open fun onRefresh(event: SaveCarroEvent) {
        taskCarros()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancela os eventos do bus
        EventBus.getDefault().unregister(this)
    }
}
