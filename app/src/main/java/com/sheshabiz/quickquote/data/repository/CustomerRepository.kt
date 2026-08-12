package com.sheshabiz.quickquote.data.repository

import com.sheshabiz.quickquote.data.db.dao.CustomerDao
import com.sheshabiz.quickquote.data.db.entity.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CustomerRepository(private val dao: CustomerDao) {
    fun observeAll(): Flow<List<Customer>> = dao.observeAll()

    suspend fun getAllOnce(): List<Customer> = dao.observeAll().first()

    suspend fun getById(id: Long): Customer? = dao.getById(id)

    suspend fun upsert(customer: Customer): Long =
        if (customer.id == 0L) dao.insert(customer) else {
            dao.update(customer)
            customer.id
        }

    suspend fun delete(customer: Customer) = dao.delete(customer)

    suspend fun deleteAll() = dao.deleteAll()
}
