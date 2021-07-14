package com.shivam.guftagoo.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.shivam.guftagoo.R
import com.shivam.guftagoo.databinding.FragmentAccountBinding
import com.shivam.guftagoo.databinding.FragmentHomeBinding
import com.shivam.guftagoo.extensions.launchActivity
import com.shivam.guftagoo.ui.call.UserMediaAdapter
import kotlinx.android.synthetic.main.fragment_account.*

class AccountFragment private constructor(): Fragment() {
    private lateinit var binding: FragmentAccountBinding

    companion object {
        @JvmStatic
        fun newInstance() = AccountFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        dummyCode()
    }

    private fun setupUI() {
        fab_settings.setOnClickListener{
            requireActivity().launchActivity<SettingsActivity>()
        }
        fab_edit_profile.setOnClickListener{
            requireActivity().launchActivity<EditProfileActivity>()
        }
    }

    private fun dummyCode() {
        rec_videos.layoutManager = GridLayoutManager(context, 3)
        rec_videos.adapter = UserMediaAdapter()
    }

}