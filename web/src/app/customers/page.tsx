"use client";

import { useMemo, useState } from "react";
import { Plus, Pencil, Trash2, Users } from "lucide-react";
import { Modal } from "@/components/ui/Modal";
import { DeleteConfirmDialog } from "@/components/ui/DeleteConfirmDialog";
import { SearchInput } from "@/components/ui/SearchInput";
import { useAppData } from "@/lib/store";
import type { Customer } from "@/lib/types";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";

export default function CustomersPage() {
  const { data, addCustomer, updateCustomer, deleteCustomer } = useAppData();
  const [modalCustomer, setModalCustomer] = useState<Customer | "new" | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Customer | null>(null);
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [address, setAddress] = useState("");
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return data.customers;
    return data.customers.filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        c.phone.toLowerCase().includes(q) ||
        c.email.toLowerCase().includes(q)
    );
  }, [data.customers, search]);

  function openNew() {
    setName("");
    setPhone("");
    setEmail("");
    setAddress("");
    setModalCustomer("new");
  }

  function openEdit(customer: Customer) {
    setName(customer.name);
    setPhone(customer.phone);
    setEmail(customer.email);
    setAddress(customer.address);
    setModalCustomer(customer);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const values = { name: name.trim(), phone: phone.trim(), email: email.trim(), address: address.trim() };
    if (modalCustomer === "new") {
      addCustomer(values);
    } else if (modalCustomer) {
      updateCustomer(modalCustomer.id, values);
    }
    setModalCustomer(null);
  }

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Customers</h1>
        <button
          type="button"
          onClick={openNew}
          className="flex items-center gap-1.5 rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white"
        >
          <Plus size={16} />
          Add customer
        </button>
      </div>

      <div className="mt-4">
        <SearchInput value={search} onChange={setSearch} placeholder="Search customers…" />
      </div>

      <div className="mt-4 flex flex-col gap-2.5">
        {filtered.map((customer) => (
          <div
            key={customer.id}
            className="flex items-center justify-between gap-3 rounded-2xl border border-line bg-surface p-4"
          >
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-tint text-sm font-bold text-brand-deep">
                {customer.name.slice(0, 2).toUpperCase()}
              </span>
              <div>
                <p className="font-semibold">{customer.name}</p>
                <p className="text-sm text-ink-faint">
                  {[customer.phone, customer.email].filter(Boolean).join(" · ") || "No contact details"}
                </p>
              </div>
            </div>
            <div className="flex gap-1.5">
              <button
                type="button"
                onClick={() => openEdit(customer)}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
              >
                <Pencil size={14} />
              </button>
              <button
                type="button"
                onClick={() => setDeleteTarget(customer)}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-faint hover:bg-error/10 hover:text-error"
              >
                <Trash2 size={14} />
              </button>
            </div>
          </div>
        ))}
        {filtered.length === 0 && (
          <div className="flex flex-col items-center gap-2 rounded-2xl border border-line bg-surface py-16 text-center text-ink-faint">
            <Users size={22} />
            {data.customers.length === 0 ? "No customers yet." : "No customers match your search."}
          </div>
        )}
      </div>

      <Modal
        open={modalCustomer !== null}
        title={modalCustomer === "new" ? "Add customer" : "Edit customer"}
        onClose={() => setModalCustomer(null)}
      >
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div>
            <label className={labelCls}>Name</label>
            <input className={`${inputCls} mt-1`} value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className={labelCls}>Phone</label>
              <input className={`${inputCls} mt-1`} value={phone} onChange={(e) => setPhone(e.target.value)} />
            </div>
            <div>
              <label className={labelCls}>Email</label>
              <input
                type="email"
                className={`${inputCls} mt-1`}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
          </div>
          <div>
            <label className={labelCls}>Address</label>
            <input className={`${inputCls} mt-1`} value={address} onChange={(e) => setAddress(e.target.value)} />
          </div>
          <button type="submit" className="rounded-xl bg-brand py-2.5 text-sm font-semibold text-white">
            {modalCustomer === "new" ? "Add customer" : "Save changes"}
          </button>
        </form>
      </Modal>

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        title="Delete this customer?"
        message={deleteTarget ? `${deleteTarget.name} will be removed from your customer list.` : ""}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteCustomer(deleteTarget.id);
          setDeleteTarget(null);
        }}
      />
    </div>
  );
}
