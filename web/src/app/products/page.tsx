import { Package } from "lucide-react";
import { ComingSoon } from "@/components/ui/ComingSoon";

export default function ProductsPage() {
  return (
    <ComingSoon
      icon={Package}
      title="Products"
      description="Your product catalogue, synced with the Android app — coming soon."
    />
  );
}
