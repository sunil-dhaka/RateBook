package com.example.ratebook.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.ratebook.R
import com.example.ratebook.data.database.entity.Product
import com.example.ratebook.databinding.ItemProductBinding
import com.example.ratebook.domain.util.PhotoManager
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val photoManager: PhotoManager,
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(product: Product) {
            binding.productName.text = product.name
            binding.productSku.text = product.sku
            binding.productPrice.text = currencyFormat.format(product.sellingPrice)

            if (product.photoPath != null) {
                val photoFile = photoManager.getPhotoFile(product.photoPath)
                binding.productImage.load(photoFile) {
                    placeholder(R.drawable.ic_photo_placeholder)
                    error(R.drawable.ic_photo_placeholder)
                }
            } else {
                binding.productImage.setImageResource(R.drawable.ic_photo_placeholder)
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
