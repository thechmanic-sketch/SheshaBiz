"use client";

import { useState } from "react";
import { Plus, Pencil, Trash2, Package } from "lucide-react";
import { Modal } from "@/components/ui/Modal";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { formatCurrency } from "@/lib/currency";
import { fileToDataUrl } from "@/lib/files";
import { useAppData } from "@/lib/store";
import type { Product } from "@/lib/types";

const inputCls =
  "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm outline-none focus:border-brand";
const labelCls = "text-xs font-semibold text-ink-faint";

function ProductFormFields({
  name,
  setName,
  price,
  setPrice,
  stockQty,
  setStockQty,
  imageDataUrl,
  setImageDataUrl,
}: {
  name: string;
  setName: (v: string) => void;
  price: string;
  setPrice: (v: string) => void;
  stockQty: string;
  setStockQty: (v: string) => void;
  imageDataUrl: string | null;
  setImageDataUrl: (v: string | null) => void;
}) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3">
        {imageDataUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={imageDataUrl} alt="" className="h-14 w-14 rounded-xl object-cover" />
        ) : (
          <span className="flex h-14 w-14 items-center justify-center rounded-xl bg-brand-tint text-brand-deep">
            <Package size={22} />
          </span>
        )}
        <label className="cursor-pointer rounded-lg border border-line px-3 py-1.5 text-xs font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]">
          Upload image
          <input
            type="file"
            accept="image/*"
            className="hidden"
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (file) setImageDataUrl(await fileToDataUrl(file));
            }}
          />
        </label>
      </div>
      <div>
        <label className={labelCls}>Name</label>
        <input className={`${inputCls} mt-1`} value={name} onChange={(e) => setName(e.target.value)} required />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className={labelCls}>Price</label>
          <input
            type="number"
            min={0}
            step="0.01"
            className={`${inputCls} mt-1 tabular`}
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            required
          />
        </div>
        <div>
          <label className={labelCls}>Stock quantity</label>
          <input
            type="number"
            min={0}
            className={`${inputCls} mt-1 tabular`}
            value={stockQty}
            onChange={(e) => setStockQty(e.target.value)}
          />
        </div>
      </div>
    </div>
  );
}

export default function ProductsPage() {
  const { data, addProduct, updateProduct, deleteProduct } = useAppData();
  const [modalProduct, setModalProduct] = useState<Product | "new" | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null);
  const [name, setName] = useState("");
  const [price, setPrice] = useState("");
  const [stockQty, setStockQty] = useState("");
  const [imageDataUrl, setImageDataUrl] = useState<string | null>(null);

  function openNew() {
    setName("");
    setPrice("");
    setStockQty("");
    setImageDataUrl(null);
    setModalProduct("new");
  }

  function openEdit(product: Product) {
    setName(product.name);
    setPrice(String(product.price));
    setStockQty(String(product.stockQty));
    setImageDataUrl(product.imageDataUrl);
    setModalProduct(product);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const values = {
      name: name.trim(),
      price: Number(price) || 0,
      stockQty: Number(stockQty) || 0,
      imageDataUrl,
    };
    if (modalProduct === "new") {
      addProduct(values);
    } else if (modalProduct) {
      updateProduct(modalProduct.id, values);
    }
    setModalProduct(null);
  }

  return (
    <div className="mx-auto max-w-5xl">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Products</h1>
        <button
          type="button"
          onClick={openNew}
          className="flex items-center gap-1.5 rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white"
        >
          <Plus size={16} />
          Add product
        </button>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {data.products.map((product) => (
          <div key={product.id} className="rounded-2xl border border-line bg-surface p-4">
            {product.imageDataUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={product.imageDataUrl} alt="" className="h-16 w-16 rounded-xl object-cover" />
            ) : (
              <span className="flex h-16 w-16 items-center justify-center rounded-xl bg-brand-tint text-brand-deep">
                <Package size={22} />
              </span>
            )}
            <p className="mt-2 text-sm font-semibold">{product.name}</p>
            <p className="text-sm font-bold tabular text-brand-deep">
              {formatCurrency(product.price, data.business.country)}
            </p>
            <p className="text-xs text-ink-faint">{product.stockQty} in stock</p>
            <div className="mt-3 flex gap-1.5">
              <button
                type="button"
                onClick={() => openEdit(product)}
                className="flex flex-1 items-center justify-center gap-1 rounded-lg border border-line py-1.5 text-xs font-semibold text-ink-soft hover:bg-black/[.04] dark:hover:bg-white/[.06]"
              >
                <Pencil size={13} />
                Edit
              </button>
              <button
                type="button"
                onClick={() => setDeleteTarget(product)}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-faint hover:bg-error/10 hover:text-error"
              >
                <Trash2 size={14} />
              </button>
            </div>
          </div>
        ))}
        {data.products.length === 0 && (
          <p className="col-span-full py-10 text-center text-ink-faint">No products yet.</p>
        )}
      </div>

      <Modal
        open={modalProduct !== null}
        title={modalProduct === "new" ? "Add product" : "Edit product"}
        onClose={() => setModalProduct(null)}
      >
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <ProductFormFields
            name={name}
            setName={setName}
            price={price}
            setPrice={setPrice}
            stockQty={stockQty}
            setStockQty={setStockQty}
            imageDataUrl={imageDataUrl}
            setImageDataUrl={setImageDataUrl}
          />
          <button type="submit" className="rounded-xl bg-brand py-2.5 text-sm font-semibold text-white">
            {modalProduct === "new" ? "Add product" : "Save changes"}
          </button>
        </form>
      </Modal>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Delete this product?"
        message={deleteTarget ? `${deleteTarget.name} will be removed from your catalog.` : ""}
        confirmLabel="Delete"
        danger
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) deleteProduct(deleteTarget.id);
          setDeleteTarget(null);
        }}
      />
    </div>
  );
}
