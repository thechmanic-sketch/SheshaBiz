import { ShoppingCart } from "lucide-react";
import { ComingSoon } from "@/components/ui/ComingSoon";

export default function PosPage() {
  return (
    <ComingSoon
      icon={ShoppingCart}
      title="Point of Sale"
      description="Ring up walk-in sales from the browser — coming soon."
    />
  );
}
